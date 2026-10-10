package com.edumind.backend.service;

import com.edumind.backend.dto.ImportResponse;
import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.FacultyRepository;
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
public class FacultyCsvImportService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public FacultyCsvImportService(
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ImportResponse importFaculty(MultipartFile file) {

        int totalRows = 0;
        int successfulRows = 0;

        List<String> errors = new ArrayList<>();

        try (
            Reader reader = new InputStreamReader(
                    file.getInputStream(),
                    StandardCharsets.UTF_8);

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader)
        ) {

            for (CSVRecord record : parser) {

                totalRows++;

                try {
                    importFacultyRecord(record);
                    successfulRows++;

                } catch (Exception e) {

                    errors.add(
                            "Row " + record.getRecordNumber()
                                    + ": " + e.getMessage()
                    );
                }
            }

        } catch (Exception e) {

            errors.add(
                    "Could not read CSV file: " + e.getMessage()
            );
        }

        int failedRows = totalRows - successfulRows;

        return new ImportResponse(
                totalRows,
                successfulRows,
                failedRows,
                errors
        );
    }

    private void importFacultyRecord(CSVRecord record) {

        String username = record.get("username");
        String password = record.get("password");
        String fullName = record.get("fullName");
        String email = record.get("email");
        String employeeId = record.get("employeeId");
        String designation = record.get("designation");

        Long departmentId =
                Long.parseLong(record.get("departmentId"));

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException(
                    "Username already exists");
        }

        if (email != null
                && !email.isBlank()
                && userRepository.findByEmail(email).isPresent()) {

            throw new RuntimeException(
                    "Email already exists");
        }

        if (facultyRepository.existsByEmployeeId(employeeId)) {
            throw new RuntimeException(
                    "Employee ID already exists");
        }

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"));

        User user = new User();

        user.setUsername(username);
        user.setPassword(
                passwordEncoder.encode(password));
        user.setRole("FACULTY");
        user.setFullName(fullName);
        user.setEmail(email);

        User savedUser = userRepository.save(user);

        Faculty faculty = new Faculty();

        faculty.setEmployeeId(employeeId);
        faculty.setDesignation(designation);
        faculty.setUser(savedUser);
        faculty.setDepartment(department);

        facultyRepository.save(faculty);
    }
}
