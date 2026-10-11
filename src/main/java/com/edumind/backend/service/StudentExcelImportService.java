
package com.edumind.backend.service;

import com.edumind.backend.dto.ImportResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentExcelImportService {

    private final StudentService studentService;

    public StudentExcelImportService(StudentService studentService) {
        this.studentService = studentService;
    }

    public ImportResponse importStudents(MultipartFile file) {

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
                    String username = getCellValue(row.getCell(0));
                    String password = getCellValue(row.getCell(1));
                    String fullName = getCellValue(row.getCell(2));
                    String email = getCellValue(row.getCell(3));
                    String rollNumber = getCellValue(row.getCell(4));

                    Long departmentId =
                            Long.parseLong(
                                    getCellValue(row.getCell(5)));

                    Long programId =
                            Long.parseLong(
                                    getCellValue(row.getCell(6)));

                    Long semesterId =
                            Long.parseLong(
                                    getCellValue(row.getCell(7)));

                    Long cohortId = parseOptionalId(row.getCell(8));
                    Long sectionId = parseOptionalId(row.getCell(9));

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
                            sectionId
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

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row) {

        for (int i = 0; i < 10; i++) {

            Cell cell = row.getCell(i);

            if (cell != null
                    && !getCellValue(cell).isBlank()) {

                return false;
            }
        }

        return true;
    }

    private Long parseOptionalId(Cell cell) {
        String value = getCellValue(cell);
        return value.isBlank() ? null : Long.parseLong(value);
    }
}