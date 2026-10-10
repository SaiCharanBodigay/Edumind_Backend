package com.edumind.backend.controller;

import com.edumind.backend.dto.AdminDashboardResponse;
import com.edumind.backend.repository.AcademicYearRepository;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final FacultyCourseAllocationRepository allocationRepository;

    public AdminController(
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            CourseRepository courseRepository,
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            FacultyCourseAllocationRepository allocationRepository) {

        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.allocationRepository = allocationRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {

        AdminDashboardResponse response = new AdminDashboardResponse(
                studentRepository.count(),
                facultyRepository.count(),
                departmentRepository.count(),
                programRepository.count(),
                courseRepository.count(),
                academicYearRepository.count(),
                semesterRepository.count(),
                allocationRepository.count()
        );

        return ResponseEntity.ok(response);
    }
}