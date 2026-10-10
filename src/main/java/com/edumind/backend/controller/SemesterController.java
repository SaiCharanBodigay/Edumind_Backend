package com.edumind.backend.controller;

import com.edumind.backend.dto.SemesterRequest;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.service.SemesterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public ResponseEntity<List<Semester>> getAllSemesters() {
        return ResponseEntity.ok(
                semesterService.getAllSemesters()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Semester> getSemesterById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                semesterService.getSemesterById(id)
        );
    }

    @PostMapping
    public ResponseEntity<Semester> createSemester(
            @Valid @RequestBody SemesterRequest request) {

        Semester created = semesterService.createSemester(
                request.getSemesterNumber(),
                request.getProgramId(),
                request.getAcademicYearId()
        );

        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Semester> updateSemester(
            @PathVariable Long id,
            @Valid @RequestBody SemesterRequest request) {

        Semester updated = semesterService.updateSemester(
                id,
                request.getSemesterNumber(),
                request.getProgramId(),
                request.getAcademicYearId()
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSemester(
            @PathVariable Long id) {

        semesterService.deleteSemester(id);

        return ResponseEntity.noContent().build();
    }
}