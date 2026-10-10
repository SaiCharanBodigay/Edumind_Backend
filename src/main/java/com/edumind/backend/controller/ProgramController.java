package com.edumind.backend.controller;

import com.edumind.backend.dto.ProgramRequest;
import com.edumind.backend.entity.Program;
import com.edumind.backend.service.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    public ResponseEntity<List<Program>> getAllPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Program> getProgramById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                programService.getProgramById(id)
        );
    }

    @PostMapping
    public ResponseEntity<Program> createProgram(
            @Valid @RequestBody ProgramRequest request) {

        Program created = programService.createProgram(
                request.getName(),
                request.getCode(),
                request.getDepartmentId()
        );

        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Program> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody ProgramRequest request) {

        Program updated = programService.updateProgram(
                id,
                request.getName(),
                request.getCode(),
                request.getDepartmentId()
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable Long id) {

        programService.deleteProgram(id);

        return ResponseEntity.noContent().build();
    }
}