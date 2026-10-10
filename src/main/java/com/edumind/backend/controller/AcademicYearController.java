package com.edumind.backend.controller;

import com.edumind.backend.dto.AcademicYearRequest;
import com.edumind.backend.entity.AcademicYear;
import com.edumind.backend.service.AcademicYearService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/academic-years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @GetMapping
    public ResponseEntity<List<AcademicYear>> getAllAcademicYears() {
        return ResponseEntity.ok(
                academicYearService.getAllAcademicYears()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicYear> getAcademicYearById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                academicYearService.getAcademicYearById(id)
        );
    }

    @PostMapping
    public ResponseEntity<AcademicYear> createAcademicYear(
            @Valid @RequestBody AcademicYearRequest request) {

        AcademicYear created =
                academicYearService.createAcademicYear(request.getName());

        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcademicYear> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody AcademicYearRequest request) {

        AcademicYear updated =
                academicYearService.updateAcademicYear(
                        id,
                        request.getName()
                );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcademicYear(
            @PathVariable Long id) {

        academicYearService.deleteAcademicYear(id);

        return ResponseEntity.noContent().build();
    }
}