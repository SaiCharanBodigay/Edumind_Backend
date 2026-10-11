package com.edumind.backend.service;

import com.edumind.backend.dto.ClassInchargeResponse;
import com.edumind.backend.dto.FacultySectionResponse;
import com.edumind.backend.dto.StudentResponse;
import com.edumind.backend.dto.StudentResponseMapper;
import com.edumind.backend.dto.StudentSearchResponse;
import com.edumind.backend.entity.ClassInchargeAssignment;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.Section;
import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.SectionRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClassInchargeService {

    private final ClassInchargeAssignmentRepository assignmentRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;

    public ClassInchargeService(
            ClassInchargeAssignmentRepository assignmentRepository,
            FacultyRepository facultyRepository,
            SectionRepository sectionRepository,
            StudentRepository studentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.facultyRepository = facultyRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassInchargeResponse> getActiveAssignments() {
        return assignmentRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassInchargeResponse assign(
            Long facultyId,
            Long sectionId,
            LocalDate requestedStartDate) {
        Section section = sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Section not found."));
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Faculty member not found."));
        LocalDate startDate = requestedStartDate == null
                ? LocalDate.now()
                : requestedStartDate;
        if (startDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Class-incharge assignments cannot start in the future.");
        }

        ClassInchargeAssignment current = assignmentRepository
                .findFirstBySectionIdAndActiveTrue(sectionId)
                .orElse(null);
        if (current != null && current.getFaculty().getId().equals(facultyId)) {
            return toResponse(current);
        }
        if (current != null) {
            if (startDate.isBefore(current.getStartDate())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The new assignment cannot start before the current assignment.");
            }
            current.end(startDate);
            assignmentRepository.save(current);
        }
        return toResponse(assignmentRepository.save(
                new ClassInchargeAssignment(faculty, section, startDate)));
    }

    @Transactional
    public void endAssignment(Long assignmentId) {
        ClassInchargeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Class-incharge assignment not found."));
        sectionRepository.findByIdForUpdate(assignment.getSection().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Section not found."));
        if (assignment.isActive()) {
            assignment.end(LocalDate.now());
            assignmentRepository.save(assignment);
        }
    }

    @Transactional(readOnly = true)
    public List<FacultySectionResponse> getMySections(String username) {
        return assignmentRepository.findAllActiveByFacultyUsername(username).stream()
                .map(assignment -> {
                    Section section = assignment.getSection();
                    return new FacultySectionResponse(
                            section.getId(),
                            section.getName(),
                            section.getCohort().getId(),
                            section.getCohort().getName(),
                            section.getCohort().getProgram().getName(),
                            section.getCohort().getProgram().getDepartment().getName(),
                            section.getSemester().getSemesterNumber(),
                            section.getSemester().getAcademicYear().getName());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentSearchResponse getMySectionStudents(
            String username,
            Long sectionId,
            String search,
            int page,
            int size) {
        if (!assignmentRepository.existsByFacultyUserUsernameAndSectionIdAndActiveTrue(
                username, sectionId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not the active class incharge for this section.");
        }
        Page<StudentResponse> results = studentRepository.search(
                        normalize(search), null, null, null, null, sectionId,
                        PageRequest.of(page, size, Sort.by("rollNumber").ascending()))
                .map(StudentResponseMapper::toResponse);
        return toSearchResponse(results);
    }

    public static StudentSearchResponse toSearchResponse(Page<StudentResponse> results) {
        return new StudentSearchResponse(
                results.getContent(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    private ClassInchargeResponse toResponse(ClassInchargeAssignment assignment) {
        Faculty faculty = assignment.getFaculty();
        Section section = assignment.getSection();
        var cohort = section.getCohort();
        var program = cohort.getProgram();
        var semester = section.getSemester();
        return new ClassInchargeResponse(
                assignment.getId(),
                faculty.getId(),
                faculty.getUser().getFullName(),
                faculty.getEmployeeId(),
                section.getId(),
                section.getName(),
                cohort.getId(),
                cohort.getName(),
                program.getId(),
                program.getName(),
                program.getDepartment().getId(),
                program.getDepartment().getName(),
                semester.getSemesterNumber(),
                semester.getAcademicYear().getName(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.isActive());
    }

    private static String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
