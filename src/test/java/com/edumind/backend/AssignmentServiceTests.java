package com.edumind.backend;

import com.edumind.backend.dto.AssignmentRequest;
import com.edumind.backend.dto.StudentPortalCourseResponse;
import com.edumind.backend.entity.AssignmentSubmission;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.CourseAssignment;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.AssignmentSubmissionRepository;
import com.edumind.backend.repository.CourseAssignmentRepository;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.AssignmentService;
import com.edumind.backend.service.AssignmentSubmissionStorageService;
import com.edumind.backend.service.StudentPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTests {

    private static final String FACULTY_USERNAME = "faculty-alpha";
    private static final String STUDENT_USERNAME = "student-alpha";
    private static final Long COURSE_ID = 31L;
    private static final Long ASSIGNMENT_ID = 41L;
    private static final Long SUBMISSION_ID = 51L;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private FacultyCourseAllocationRepository allocationRepository;

    @Mock
    private CourseAssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @Mock
    private StudentPortalService studentPortalService;

    @Mock
    private AssignmentSubmissionStorageService storageService;

    private Faculty faculty;
    private Student student;
    private Course course;
    private CourseAssignment assignment;
    private AssignmentService service;

    @BeforeEach
    void setUp() {
        faculty = new Faculty();
        faculty.setId(12L);
        student = new Student(
                "ST-100",
                new User(STUDENT_USERNAME, "unused", "STUDENT", "Student Alpha", null),
                null,
                null,
                null);
        course = new Course();
        course.setId(COURSE_ID);
        course.setCode("DS");
        course.setName("Data Structures");

        assignment = org.mockito.Mockito.mock(CourseAssignment.class);
        lenient().when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        lenient().when(assignment.getCourse()).thenReturn(course);
        lenient().when(assignment.getTitle()).thenReturn("Project");
        lenient().when(assignment.getDescription()).thenReturn("Description");
        lenient().when(assignment.getInstructions()).thenReturn("Follow the rubric.");
        lenient().when(assignment.getDueAt()).thenReturn(Instant.now().plusSeconds(3600));
        lenient().when(assignment.getMaxMarks()).thenReturn(new BigDecimal("50.00"));
        lenient().when(assignment.getCreatedAt()).thenReturn(Instant.now());
        lenient().when(assignment.getUpdatedAt()).thenReturn(Instant.now());

        service = new AssignmentService(
                facultyRepository,
                studentRepository,
                courseRepository,
                allocationRepository,
                assignmentRepository,
                submissionRepository,
                studentPortalService,
                storageService);
    }

    @Test
    void facultyCanReadTheirOwnAssignment() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(assignmentRepository.findByIdAndFacultyId(ASSIGNMENT_ID, 12L))
                .thenReturn(Optional.of(assignment));
        when(allocationRepository.existsByFacultyIdAndCourseId(12L, COURSE_ID))
                .thenReturn(true);
        when(submissionRepository.countByAssignmentId(ASSIGNMENT_ID)).thenReturn(2L);

        var result = service.getForFaculty(FACULTY_USERNAME, ASSIGNMENT_ID);

        assertEquals("Project", result.title());
        assertEquals(2L, result.submissionCount());
    }

    @Test
    void facultyCannotReadAnotherFacultysAssignment() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(assignmentRepository.findByIdAndFacultyId(ASSIGNMENT_ID, 12L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getForFaculty(FACULTY_USERNAME, ASSIGNMENT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void facultyCannotCreateAssignmentForUnassignedCourse() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(allocationRepository.existsByFacultyIdAndCourseId(12L, COURSE_ID))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(FACULTY_USERNAME, COURSE_ID, validRequest()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void rejectsAssignmentWithPastDueDate() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(allocationRepository.existsByFacultyIdAndCourseId(12L, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        AssignmentRequest pastDue = new AssignmentRequest(
                "Past assignment",
                null,
                "Instructions",
                Instant.now().minusSeconds(10),
                new BigDecimal("20"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(FACULTY_USERNAME, COURSE_ID, pastDue));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void studentSeesAssignmentsOnlyFromAuthorizedCourseIds() {
        when(studentPortalService.getCourses(STUDENT_USERNAME))
                .thenReturn(List.of(new StudentPortalCourseResponse(
                        COURSE_ID, "DS", "Data Structures", 4, 3,
                        "B.Tech CSE", "2026-27", List.of(), 0)));
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));
        when(assignmentRepository.findAllByCourseIdInOrderByDueAtAsc(List.of(COURSE_ID)))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT_ID, null))
                .thenReturn(Optional.empty());

        var result = service.listForStudent(STUDENT_USERNAME);

        assertEquals(1, result.size());
        assertEquals("NOT_SUBMITTED", result.getFirst().submissionStatus());
        verify(assignmentRepository).findAllByCourseIdInOrderByDueAtAsc(List.of(COURSE_ID));
    }

    @Test
    void studentCannotOpenAssignmentForUnauthorizedCourse() {
        when(assignmentRepository.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(assignment));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getForStudent(STUDENT_USERNAME, ASSIGNMENT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(studentRepository, never()).findByUserUsername(STUDENT_USERNAME);
    }

    @Test
    void lateSubmissionIsRejectedBeforeStorage() {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(assignment.getDueAt()).thenReturn(Instant.now().minusSeconds(1));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenReturn(course);
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submit(STUDENT_USERNAME, ASSIGNMENT_ID, textFile()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(storageService, never()).store(any());
    }

    @Test
    void duplicateSubmissionIsRejected() {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenReturn(course);
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));
        when(submissionRepository.existsByAssignmentIdAndStudentId(ASSIGNMENT_ID, null))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submit(STUDENT_USERNAME, ASSIGNMENT_ID, textFile()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(storageService, never()).store(any());
    }

    @Test
    void concurrentDuplicateSubmissionIsRejectedAndStoredFileIsRemoved() {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenReturn(course);
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));
        when(submissionRepository.existsByAssignmentIdAndStudentId(ASSIGNMENT_ID, null))
                .thenReturn(false);
        when(storageService.store(any()))
                .thenReturn(new AssignmentSubmissionStorageService.StoredFile(
                        "work.txt", "text/plain", 4L, "generated-name"));
        doThrow(new DataIntegrityViolationException("duplicate submission"))
                .when(submissionRepository).saveAndFlush(any());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submit(STUDENT_USERNAME, ASSIGNMENT_ID, textFile()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(storageService).delete("generated-name");
    }

    @Test
    void gradingAboveMaximumIsRejected() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(assignmentRepository.findByIdAndFacultyId(ASSIGNMENT_ID, 12L))
                .thenReturn(Optional.of(assignment));
        when(allocationRepository.existsByFacultyIdAndCourseId(12L, COURSE_ID))
                .thenReturn(true);
        when(submissionRepository.findByIdAndAssignmentId(SUBMISSION_ID, ASSIGNMENT_ID))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(AssignmentSubmission.class)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.grade(
                        FACULTY_USERNAME,
                        ASSIGNMENT_ID,
                        SUBMISSION_ID,
                        new com.edumind.backend.dto.GradeSubmissionRequest(
                                new BigDecimal("50.01"), "Too high", false)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void unreleasedGradeAndFeedbackAreNotReturnedToStudent() {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenReturn(course);
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));
        AssignmentSubmission submission = new AssignmentSubmission(
                assignment, student, "work.txt", "text/plain", 5L, "generated.txt");
        submission.grade(new BigDecimal("40.00"), "Private feedback", false);
        when(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT_ID, null))
                .thenReturn(Optional.of(submission));

        var result = service.getForStudent(STUDENT_USERNAME, ASSIGNMENT_ID);

        assertAll(
                () -> assertEquals("SUBMITTED", result.submissionStatus()),
                () -> assertNull(result.releasedMarks()),
                () -> assertNull(result.releasedFeedback()));
    }

    @Test
    void releasedGradeAndFeedbackAreVisibleToOwner() {
        when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(studentPortalService.requireStudentCourse(STUDENT_USERNAME, COURSE_ID))
                .thenReturn(course);
        when(studentRepository.findByUserUsername(STUDENT_USERNAME))
                .thenReturn(Optional.of(student));
        AssignmentSubmission submission = new AssignmentSubmission(
                assignment, student, "work.txt", "text/plain", 5L, "generated.txt");
        submission.grade(new BigDecimal("40.00"), "Good work", true);
        when(submissionRepository.findByAssignmentIdAndStudentId(ASSIGNMENT_ID, null))
                .thenReturn(Optional.of(submission));

        var result = service.getForStudent(STUDENT_USERNAME, ASSIGNMENT_ID);

        assertEquals("GRADED", result.submissionStatus());
        assertEquals(new BigDecimal("40.00"), result.releasedMarks());
        assertEquals("Good work", result.releasedFeedback());
    }

    @Test
    void facultyCannotReviewAssignmentOwnedByAnotherFaculty() {
        when(facultyRepository.findByUserUsername(FACULTY_USERNAME))
                .thenReturn(Optional.of(faculty));
        when(assignmentRepository.findByIdAndFacultyId(ASSIGNMENT_ID, 12L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listSubmissions(FACULTY_USERNAME, ASSIGNMENT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(submissionRepository, never())
                .findAllByAssignmentIdOrderBySubmittedAtAsc(ASSIGNMENT_ID);
    }

    private AssignmentRequest validRequest() {
        return new AssignmentRequest(
                "Project",
                "Description",
                "Follow the rubric.",
                Instant.now().plusSeconds(3600),
                new BigDecimal("50.00"));
    }

    private MockMultipartFile textFile() {
        return new MockMultipartFile(
                "file", "work.txt", "text/plain", "work".getBytes());
    }
}
