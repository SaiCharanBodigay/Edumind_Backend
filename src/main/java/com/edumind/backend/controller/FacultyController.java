package com.edumind.backend.controller;

import com.edumind.backend.dto.FacultyRequest;
import com.edumind.backend.dto.FacultyResponse;
import com.edumind.backend.dto.FacultyUpdateRequest;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.service.FacultyService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> getAllFaculty() {

        List<FacultyResponse> response =
                facultyService.getAllFaculty()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getFacultyById(
            @PathVariable Long id) {

        Faculty faculty = facultyService.getFacultyById(id);

        return ResponseEntity.ok(toResponse(faculty));
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
                .body(toResponse(faculty));
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

        return ResponseEntity.ok(toResponse(faculty));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaculty(
            @PathVariable Long id) {

        facultyService.deleteFaculty(id);

        return ResponseEntity.noContent().build();
    }

    private FacultyResponse toResponse(Faculty faculty) {

        return new FacultyResponse(
                faculty.getId(),
                faculty.getEmployeeId(),
                faculty.getDesignation(),

                faculty.getUser().getUsername(),
                faculty.getUser().getFullName(),
                faculty.getUser().getEmail(),

                faculty.getDepartment().getId(),
                faculty.getDepartment().getName(),
                faculty.getDepartment().getCode()
        );
    }
}
