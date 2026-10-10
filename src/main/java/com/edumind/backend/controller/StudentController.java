package com.edumind.backend.controller;

import com.edumind.backend.dto.StudentRequest;
import com.edumind.backend.dto.StudentResponse;
import com.edumind.backend.dto.StudentUpdateRequest;
import com.edumind.backend.entity.Student;
import com.edumind.backend.service.StudentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        List<StudentResponse> response = studentService.getAllStudents()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        Student student = studentService.getStudentById(id);

        return ResponseEntity.ok(toResponse(student));
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
                request.getSemesterId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(student));
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
            request.getSemesterId());

    return ResponseEntity.ok(toResponse(student));
}
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {

    studentService.deleteStudent(id);

    return ResponseEntity.noContent().build();
}
    private StudentResponse toResponse(Student student) {

        return new StudentResponse(
                student.getId(),
                student.getRollNumber(),

                student.getUser().getUsername(),
                student.getUser().getFullName(),
                student.getUser().getEmail(),

                student.getDepartment().getId(),
                student.getDepartment().getName(),
                student.getDepartment().getCode(),

                student.getProgram().getId(),
                student.getProgram().getName(),
                student.getProgram().getCode(),

                student.getSemester().getId(),
                student.getSemester().getSemesterNumber()
        );
    }
}
