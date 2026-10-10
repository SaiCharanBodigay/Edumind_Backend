package com.edumind.backend.controller;

import com.edumind.backend.dto.ImportResponse;
import com.edumind.backend.service.StudentCsvImportService;
import com.edumind.backend.service.StudentExcelImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
public class StudentCsvImportController {

    private final StudentCsvImportService importService;
    private final StudentExcelImportService excelImportService;

    public StudentCsvImportController(
            StudentCsvImportService importService,
            StudentExcelImportService excelImportService) {

        this.importService = importService;
        this.excelImportService = excelImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importStudents(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportResponse(
                            0,
                            0,
                            0,
                            List.of("CSV file is empty")
                    )
            );
        }

        ImportResponse response =
                importService.importStudents(file);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/import-excel")
    public ResponseEntity<ImportResponse> importStudentsFromExcel(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ImportResponse(
                            0,
                            0,
                            0,
                            List.of("Excel file is empty")
                    )
            );
        }

        ImportResponse response =
                excelImportService.importStudents(file);

        return ResponseEntity.ok(response);
    }
}
