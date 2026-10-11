package com.edumind.backend.service;

import com.edumind.backend.dto.ImportResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentCsvImportService {

    private final StudentService studentService;

    public StudentCsvImportService(StudentService studentService) {
        this.studentService = studentService;
    }

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

        Long cohortId = optionalId(record, "cohortId");
        Long sectionId = optionalId(record, "sectionId");

        studentService.createStudent(
                username,
                password,
                fullName,
                email,
                rollNumber,
                departmentId,
                programId,
                semesterId,
                cohortId,
                sectionId);
    }

    private Long optionalId(CSVRecord record, String column) {
        if (!record.isMapped(column) || record.get(column).isBlank()) {
            return null;
        }
        return Long.parseLong(record.get(column));
    }
}