package com.edumind.backend.service;

import com.edumind.backend.dto.ImportResponse;
import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.repository.UserRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentCsvImportService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentCsvImportService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            SemesterRepository semesterRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.semesterRepository = semesterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ImportResponse importStudents(MultipartFile file) {

        int totalRows = 0;
        int successfulRows = 0;

        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8);

             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {

                totalRows++;

                try {
                    importStudent(record);
                    successfulRows++;

                } catch (Exception e) {
                    errors.add(
                            "Row " + record.getRecordNumber()
                                    + ": " + e.getMessage()
                    );
                }
            }

        } catch (Exception e) {

            errors.add("Could not read CSV file: " + e.getMessage());
        }

        int failedRows = totalRows - successfulRows;

        return new ImportResponse(
                totalRows,
                successfulRows,
                failedRows,
                errors
        );
    }

    private void importStudent(CSVRecord record) {

        String username = record.get("username");
        String password = record.get("password");
        String fullName = record.get("fullName");
        String email = record.get("email");
        String rollNumber = record.get("rollNumber");

        Long departmentId =
                Long.parseLong(record.get("departmentId"));

        Long programId =
                Long.parseLong(record.get("programId"));

        Long semesterId =
                Long.parseLong(record.get("semesterId"));

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (email != null
                && !email.isBlank()
                && userRepository.findByEmail(email).isPresent()) {

            throw new RuntimeException("Email already exists");
        }

        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw new RuntimeException("Roll number already exists");
        }

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"));

        Program program =
                programRepository.findById(programId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Program not found"));

        Semester semester =
                semesterRepository.findById(semesterId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Semester not found"));

        if (!program.getDepartment().getId()
                .equals(department.getId())) {

            throw new RuntimeException(
                    "Program does not belong to selected department");
        }

        if (!semester.getProgram().getId()
                .equals(program.getId())) {

            throw new RuntimeException(
                    "Semester does not belong to selected program");
        }

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setFullName(fullName);
        user.setEmail(email);

        User savedUser = userRepository.save(user);

        Student student = new Student();

        student.setRollNumber(rollNumber);
        student.setUser(savedUser);
        student.setDepartment(department);
        student.setProgram(program);
        student.setSemester(semester);

        studentRepository.save(student);
    }
}