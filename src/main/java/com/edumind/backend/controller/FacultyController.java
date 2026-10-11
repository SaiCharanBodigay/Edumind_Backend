package com.edumind.backend.controller;

import com.edumind.backend.dto.FacultyRequest;
import com.edumind.backend.dto.FacultyResponse;
import com.edumind.backend.dto.FacultyUpdateRequest;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.service.FacultyService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final ClassInchargeAssignmentRepository inchargeRepository;

    public FacultyController(
            FacultyService facultyService,
            ClassInchargeAssignmentRepository inchargeRepository) {
        this.facultyService = facultyService;
        this.inchargeRepository = inchargeRepository;
    }

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> getAllFaculty() {

        Map<Long, List<String>> activeSections = inchargeRepository.findAllActive().stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getFaculty().getId(),
                        Collectors.mapping(
                                assignment -> assignment.getSection().getName()
                                        + " · " + assignment.getSection().getCohort().getName()
                                        + " · Semester "
                                        + assignment.getSection().getSemester().getSemesterNumber(),
                                Collectors.toList())));
        List<FacultyResponse> response =
                facultyService.getAllFaculty()
                        .stream()
                        .map(faculty -> toResponse(
                                faculty,
                                activeSections.getOrDefault(faculty.getId(), List.of())))
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getFacultyById(
            @PathVariable Long id) {

        Faculty faculty = facultyService.getFacultyById(id);

        return ResponseEntity.ok(toResponse(faculty, getActiveSections(faculty.getId())));
    }

    @PostMapping
    public ResponseEntity<FacultyResponse> createFaculty(
            @Valid @RequestBody FacultyRequest request) {

        Faculty faculty = facultyService.createFaculty(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail(),
                request.getEmployeeId(),
                request.getDesignation(),
                request.getDepartmentId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(faculty, getActiveSections(faculty.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacultyResponse> updateFaculty(
            @PathVariable Long id,
            @Valid @RequestBody FacultyUpdateRequest request) {

        Faculty faculty = facultyService.updateFaculty(
                id,
                request.getFullName(),
                request.getEmail(),
                request.getEmployeeId(),
                request.getDesignation(),
                request.getDepartmentId()
        );

        return ResponseEntity.ok(toResponse(faculty, getActiveSections(faculty.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaculty(
            @PathVariable Long id) {

        facultyService.deleteFaculty(id);

        return ResponseEntity.noContent().build();
    }

    private List<String> getActiveSections(Long facultyId) {
        return inchargeRepository.findAllByFacultyIdAndActiveTrue(facultyId).stream()
                .map(assignment -> assignment.getSection().getName()
                        + " · " + assignment.getSection().getCohort().getName()
                        + " · Semester "
                        + assignment.getSection().getSemester().getSemesterNumber())
                .toList();
    }

    private FacultyResponse toResponse(Faculty faculty, List<String> activeSections) {

        return new FacultyResponse(
                faculty.getId(),
                faculty.getEmployeeId(),
                faculty.getDesignation(),

                faculty.getUser().getUsername(),
                faculty.getUser().getFullName(),
                faculty.getUser().getEmail(),

                faculty.getDepartment().getId(),
                faculty.getDepartment().getName(),
                faculty.getDepartment().getCode(),
                activeSections
        );
    }
}
