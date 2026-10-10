package com.edumind.backend.service;

import com.edumind.backend.dto.ImportResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class FacultyExcelImportService {

    private final FacultyService facultyService;

    public FacultyExcelImportService(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    public ImportResponse importFaculty(MultipartFile file) {

        int totalRows = 0;
        int successfulRows = 0;

        List<String> errors = new ArrayList<>();

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;

                try {
                    String username =
                            getCellValue(row.getCell(0));

                    String password =
                            getCellValue(row.getCell(1));

                    String fullName =
                            getCellValue(row.getCell(2));

                    String email =
                            getCellValue(row.getCell(3));

                    String employeeId =
                            getCellValue(row.getCell(4));

                    String designation =
                            getCellValue(row.getCell(5));

                    Long departmentId =
                            Long.parseLong(
                                    getCellValue(row.getCell(6)));

                    facultyService.createFaculty(
                            username,
                            password,
                            fullName,
                            email,
                            employeeId,
                            designation,
                            departmentId
                    );

                    successfulRows++;

                } catch (Exception e) {

                    errors.add(
                            "Row " + (rowIndex + 1)
                                    + ": " + e.getMessage()
                    );
                }
            }

        } catch (Exception e) {

            errors.add(
                    "Could not read Excel file: "
                            + e.getMessage()
            );
        }

        return new ImportResponse(
                totalRows,
                successfulRows,
                totalRows - successfulRows,
                errors
        );
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter =
                new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row) {

        for (int i = 0; i < 7; i++) {

            Cell cell = row.getCell(i);

            if (cell != null
                    && !getCellValue(cell).isBlank()) {

                return false;
            }
        }

        return true;
    }
}