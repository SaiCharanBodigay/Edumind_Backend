package com.edumind.backend.controller;

import com.edumind.backend.dto.StudentRequest;
import com.edumind.backend.dto.StudentResponse;
import com.edumind.backend.dto.StudentUpdateRequest;
import com.edumind.backend.dto.StudentPlacementRequest;
import com.edumind.backend.dto.StudentResponseMapper;
import com.edumind.backend.dto.StudentSearchResponse;
import com.edumind.backend.entity.Student;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.StudentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;

    public StudentController(StudentService studentService, StudentRepository studentRepository) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        List<StudentResponse> response = studentService.getAllStudents()
                .stream()
                .map(StudentResponseMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<StudentSearchResponse> searchStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        Page<StudentResponse> results = studentRepository.search(
                        search == null || search.isBlank() ? null : search.trim(),
                        departmentId, programId, cohortId, semesterId, sectionId,
                        PageRequest.of(page, size, Sort.by("rollNumber").ascending()))
                .map(StudentResponseMapper::toResponse);
        return ResponseEntity.ok(new StudentSearchResponse(
                results.getContent(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        Student student = studentService.getStudentById(id);

        return ResponseEntity.ok(StudentResponseMapper.toResponse(student));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentRequest request) {

        Student student = studentService.createStudent(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail(),
                request.getRollNumber(),
                request.getDepartmentId(),
                request.getProgramId(),
                request.getSemesterId(),
                request.getCohortId(),
                request.getSectionId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StudentResponseMapper.toResponse(student));
    }
@PutMapping("/{id}")
public ResponseEntity<StudentResponse> updateStudent(
        @PathVariable Long id,
        @Valid @RequestBody StudentUpdateRequest request) {

    Student student = studentService.updateStudent(
            id,
            request.getFullName(),
            request.getEmail(),
            request.getRollNumber(),
            request.getDepartmentId(),
            request.getProgramId(),
            request.getSemesterId(),
            request.getCohortId(),
            request.getSectionId());

    return ResponseEntity.ok(StudentResponseMapper.toResponse(student));
}

@PutMapping("/{id}/placement")
public ResponseEntity<StudentResponse> assignPlacement(
        @PathVariable Long id,
        @Valid @RequestBody StudentPlacementRequest request) {
    studentService.assignPlacement(
            id, request.cohortId(), request.semesterId(), request.sectionId());
    return ResponseEntity.ok(StudentResponseMapper.toResponse(
            studentService.getStudentById(id)));
}
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {

    studentService.deleteStudent(id);

    return ResponseEntity.noContent().build();
}
}
