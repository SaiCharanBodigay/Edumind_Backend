package com.edumind.backend.service;

import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final FacultyCourseAllocationRepository allocationRepository;
    private final ClassInchargeAssignmentRepository inchargeRepository;

    public FacultyService(
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            FacultyCourseAllocationRepository allocationRepository,
            ClassInchargeAssignmentRepository inchargeRepository) {

        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.allocationRepository = allocationRepository;
        this.inchargeRepository = inchargeRepository;
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Faculty not found with id: " + id));
    }

    @Transactional
    public Faculty createFaculty(
            String username,
            String password,
            String fullName,
            String email,
            String employeeId,
            String designation,
            Long departmentId) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (email != null && !email.isBlank()
                && userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (facultyRepository.existsByEmployeeId(employeeId)) {
            throw new RuntimeException("Employee ID already exists");
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new RuntimeException("Department not found"));

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("FACULTY");
        user.setFullName(fullName);
        user.setEmail(email);

        User savedUser = userRepository.save(user);

        Faculty faculty = new Faculty();

        faculty.setEmployeeId(employeeId);
        faculty.setDesignation(designation);
        faculty.setUser(savedUser);
        faculty.setDepartment(department);

        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty updateFaculty(
            Long id,
            String fullName,
            String email,
            String employeeId,
            String designation,
            Long departmentId) {

        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Faculty not found"));

        User user = faculty.getUser();

        // Check employee ID only if it changed
        if (!faculty.getEmployeeId().equals(employeeId)
                && facultyRepository.existsByEmployeeId(employeeId)) {

            throw new RuntimeException(
                    "Employee ID already exists");
        }

        // Check email only if it changed
        if (email != null
                && !email.equals(user.getEmail())
                && userRepository.findByEmail(email).isPresent()) {

            throw new RuntimeException(
                    "Email already exists");
        }

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"));

        faculty.setEmployeeId(employeeId);
        faculty.setDesignation(designation);
        faculty.setDepartment(department);

        user.setFullName(fullName);
        user.setEmail(email);

        userRepository.save(user);

        return facultyRepository.save(faculty);
    }

    @Transactional
    public void deleteFaculty(Long id) {

        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Faculty not found"));

        // Don't delete faculty if they are assigned to courses
        if (allocationRepository.existsByFacultyId(id)) {
            throw new RuntimeException(
                    "Cannot delete faculty because they are assigned to one or more courses");
        }
        if (inchargeRepository.existsByFacultyId(id)) {
            throw new RuntimeException(
                    "Cannot delete faculty because they have current or historical class-incharge assignments");
        }

        User user = faculty.getUser();

        facultyRepository.delete(faculty);
        userRepository.delete(user);
    }
}