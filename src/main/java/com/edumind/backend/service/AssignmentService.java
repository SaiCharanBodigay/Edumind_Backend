package com.edumind.backend.service;

import com.edumind.backend.dto.AssignmentRequest;
import com.edumind.backend.dto.AssignmentResponse;
import com.edumind.backend.dto.AssignmentSubmissionResponse;
import com.edumind.backend.dto.GradeSubmissionRequest;
import com.edumind.backend.dto.StudentAssignmentResponse;
import com.edumind.backend.entity.AssignmentSubmission;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.CourseAssignment;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.Student;
import com.edumind.backend.repository.AssignmentSubmissionRepository;
import com.edumind.backend.repository.CourseAssignmentRepository;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AssignmentService {

    private static final BigDecimal MAX_ALLOWED_MARKS = new BigDecimal("1000000.00");

    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseAllocationRepository allocationRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final StudentPortalService studentPortalService;
    private final AssignmentSubmissionStorageService storageService;

    public AssignmentService(
            FacultyRepository facultyRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            FacultyCourseAllocationRepository allocationRepository,
            CourseAssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            StudentPortalService studentPortalService,
            AssignmentSubmissionStorageService storageService) {
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.studentPortalService = studentPortalService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listForFaculty(String username, Long courseId) {
        Faculty faculty = requireFaculty(username);
        requireAssignedCourse(faculty, courseId);
        return assignmentRepository
                .findAllByCourseIdAndFacultyIdOrderByDueAtAsc(courseId, faculty.getId())
                .stream()
                .map(this::toFacultyResponse)
                .toList();
    }

    @Transactional
    public AssignmentResponse create(
            String username,
            Long courseId,
            AssignmentRequest request) {
        Faculty faculty = requireFaculty(username);
        Course course = requireAssignedCourse(faculty, courseId);
        validateAssignmentFields(request);
        CourseAssignment assignment = new CourseAssignment(
                course,
                faculty,
                request.title().trim(),
                normalizeDescription(request.description()),
                request.instructions().trim(),
                request.dueAt(),
                request.maxMarks());
        return toFacultyResponse(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getForFaculty(String username, Long assignmentId) {
        return toFacultyResponse(requireFacultyAssignment(username, assignmentId));
    }

    @Transactional
    public AssignmentResponse update(
            String username,
            Long assignmentId,
            AssignmentRequest request) {
        CourseAssignment assignment = requireFacultyAssignment(username, assignmentId);
        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An assignment cannot be edited after a student submission exists.");
        }
        validateAssignmentFields(request);
        assignment.update(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.instructions().trim(),
                request.dueAt(),
                request.maxMarks());
        return toFacultyResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void delete(String username, Long assignmentId) {
        CourseAssignment assignment = requireFacultyAssignment(username, assignmentId);
        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An assignment with submissions cannot be deleted.");
        }
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<StudentAssignmentResponse> listForStudent(String username) {
        List<Long> courseIds = studentPortalService.getCourses(username).stream()
                .map(course -> course.getId())
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Student student = requireStudent(username);
        return assignmentRepository.findAllByCourseIdInOrderByDueAtAsc(courseIds)
                .stream()
                .map(assignment -> toStudentResponse(
                        assignment,
                        submissionRepository.findByAssignmentIdAndStudentId(
                                assignment.getId(), student.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentAssignmentResponse getForStudent(String username, Long assignmentId) {
        CourseAssignment assignment = requireAssignment(assignmentId);
        studentPortalService.requireStudentCourse(
                username, assignment.getCourse().getId());
        Student student = requireStudent(username);
        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElse(null);
        return toStudentResponse(assignment, submission);
    }

    @Transactional
    public StudentAssignmentResponse submit(
            String username,
            Long assignmentId,
            MultipartFile file) {
        CourseAssignment assignment = requireAssignment(assignmentId);
        studentPortalService.requireStudentCourse(
                username, assignment.getCourse().getId());
        Student student = requireStudent(username);

        if (!Instant.now().isBefore(assignment.getDueAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The submission deadline has passed.");
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(
                assignmentId, student.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A submission already exists. Resubmission is not allowed.");
        }

        AssignmentSubmissionStorageService.StoredFile stored = storageService.store(file);
        AssignmentSubmission submission = new AssignmentSubmission(
                assignment,
                student,
                stored.originalFilename(),
                stored.contentType(),
                stored.size(),
                stored.storageName());
        try {
            AssignmentSubmission saved = submissionRepository.saveAndFlush(submission);
            deleteStoredFileIfTransactionRollsBack(stored.storageName());
            return toStudentResponse(assignment, saved);
        } catch (DataIntegrityViolationException exception) {
            storageService.delete(stored.storageName());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A submission already exists. Resubmission is not allowed.",
                    exception);
        } catch (RuntimeException exception) {
            storageService.delete(stored.storageName());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listSubmissions(
            String username,
            Long assignmentId) {
        requireFacultyAssignment(username, assignmentId);
        return submissionRepository
                .findAllByAssignmentIdOrderBySubmittedAtAsc(assignmentId)
                .stream()
                .map(this::toFacultySubmissionResponse)
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse grade(
            String username,
            Long assignmentId,
            Long submissionId,
            GradeSubmissionRequest request) {
        CourseAssignment assignment = requireFacultyAssignment(username, assignmentId);
        AssignmentSubmission submission = submissionRepository
                .findByIdAndAssignmentId(submissionId, assignmentId)
                .orElseThrow(() -> notFound("Submission not found."));
        if (request.marks().compareTo(BigDecimal.ZERO) < 0
                || request.marks().compareTo(assignment.getMaxMarks()) > 0
                || request.marks().compareTo(MAX_ALLOWED_MARKS) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Marks must be between 0 and " + assignment.getMaxMarks() + ".");
        }
        submission.grade(
                request.marks(),
                normalizeDescription(request.feedback()),
                request.releaseGrade());
        return toFacultySubmissionResponse(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getOwnSubmission(
            String username,
            Long assignmentId) {
        CourseAssignment assignment = requireAssignment(assignmentId);
        studentPortalService.requireStudentCourse(
                username, assignment.getCourse().getId());
        Student student = requireStudent(username);
        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseThrow(() -> notFound("Submission not found."));
        return toStudentSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionDownload getFacultySubmissionDownload(
            String username,
            Long assignmentId,
            Long submissionId) {
        requireFacultyAssignment(username, assignmentId);
        AssignmentSubmission submission = submissionRepository
                .findByIdAndAssignmentId(submissionId, assignmentId)
                .orElseThrow(() -> notFound("Submission not found."));
        return new SubmissionDownload(
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize(),
                storageService.open(submission.getStorageName()));
    }

    @Transactional(readOnly = true)
    public SubmissionDownload getStudentSubmissionDownload(
            String username,
            Long assignmentId,
            Long submissionId) {
        CourseAssignment assignment = requireAssignment(assignmentId);
        studentPortalService.requireStudentCourse(
                username, assignment.getCourse().getId());
        Student student = requireStudent(username);
        AssignmentSubmission submission = submissionRepository
                .findByIdAndStudentId(submissionId, student.getId())
                .filter(item -> item.getAssignment().getId().equals(assignmentId))
                .orElseThrow(() -> notFound("Submission not found."));
        return new SubmissionDownload(
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize(),
                storageService.open(submission.getStorageName()));
    }

    private void validateAssignmentFields(AssignmentRequest request) {
        if (request.dueAt() == null || !request.dueAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The due date and time must be in the future.");
        }
        if (request.maxMarks() == null
                || request.maxMarks().compareTo(BigDecimal.ZERO) <= 0
                || request.maxMarks().compareTo(MAX_ALLOWED_MARKS) > 0
                || request.maxMarks().stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum marks must be positive, at most 1,000,000, and use no more than two decimal places.");
        }
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Faculty requireFaculty(String username) {
        return facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> notFound("Faculty profile not found."));
    }

    private Student requireStudent(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> notFound("Student profile not found."));
    }

    private Course requireAssignedCourse(Faculty faculty, Long courseId) {
        if (!allocationRepository.existsByFacultyIdAndCourseId(faculty.getId(), courseId)) {
            throw notFound("Assigned course not found.");
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> notFound("Assigned course not found."));
    }

    private CourseAssignment requireFacultyAssignment(String username, Long assignmentId) {
        Faculty faculty = requireFaculty(username);
        CourseAssignment assignment = assignmentRepository
                .findByIdAndFacultyId(assignmentId, faculty.getId())
                .orElseThrow(() -> notFound("Assignment not found."));
        if (!allocationRepository.existsByFacultyIdAndCourseId(
                faculty.getId(), assignment.getCourse().getId())) {
            throw notFound("Assignment not found.");
        }
        return assignment;
    }

    private CourseAssignment requireAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> notFound("Assignment not found."));
    }

    private AssignmentResponse toFacultyResponse(CourseAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getCode(),
                assignment.getCourse().getName(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getInstructions(),
                assignment.getDueAt(),
                assignment.getMaxMarks(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt(),
                submissionRepository.countByAssignmentId(assignment.getId()));
    }

    private StudentAssignmentResponse toStudentResponse(
            CourseAssignment assignment,
            AssignmentSubmission submission) {
        String status;
        if (submission != null) {
            status = submission.isGradeReleased() ? "GRADED" : "SUBMITTED";
        } else {
            status = Instant.now().isBefore(assignment.getDueAt())
                    ? "NOT_SUBMITTED"
                    : "MISSED";
        }
        boolean released = submission != null && submission.isGradeReleased();
        return new StudentAssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getCode(),
                assignment.getCourse().getName(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getInstructions(),
                assignment.getDueAt(),
                assignment.getMaxMarks(),
                status,
                submission == null ? null : submission.getId(),
                submission == null ? null : submission.getOriginalFilename(),
                submission == null ? null : submission.getSubmittedAt(),
                released ? submission.getMarks() : null,
                released ? submission.getFeedback() : null,
                released ? submission.getGradedAt() : null);
    }

    private AssignmentSubmissionResponse toFacultySubmissionResponse(
            AssignmentSubmission submission) {
        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getUser().getFullName(),
                submission.getStudent().getRollNumber(),
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize(),
                submission.getSubmittedAt(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.isGradeReleased(),
                submission.getGradedAt());
    }

    private AssignmentSubmissionResponse toStudentSubmissionResponse(
            AssignmentSubmission submission) {
        boolean released = submission.isGradeReleased();
        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                null,
                null,
                null,
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize(),
                submission.getSubmittedAt(),
                released ? submission.getMarks() : null,
                released ? submission.getFeedback() : null,
                released,
                released ? submission.getGradedAt() : null);
    }

    private void deleteStoredFileIfTransactionRollsBack(String storageName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                storageService.delete(storageName);
                            }
                        }
                    });
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record SubmissionDownload(
            String filename,
            String contentType,
            Long size,
            Resource resource) {
    }
}
