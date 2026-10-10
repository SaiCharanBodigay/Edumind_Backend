package com.edumind.backend.controller;

import com.edumind.backend.dto.ImportResponse;
import com.edumind.backend.service.FacultyCsvImportService;
import com.edumind.backend.service.FacultyExcelImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faculty")
public class FacultyCsvImportController {

    private final FacultyCsvImportService importService;
    private final FacultyExcelImportService excelImportService;

    public FacultyCsvImportController(
            FacultyCsvImportService importService,
            FacultyExcelImportService excelImportService) {

        this.importService = importService;
        this.excelImportService = excelImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importFaculty(
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
                importService.importFaculty(file);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/import-excel")
    public ResponseEntity<ImportResponse> importFacultyFromExcel(
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
                excelImportService.importFaculty(file);

        return ResponseEntity.ok(response);
    }
}