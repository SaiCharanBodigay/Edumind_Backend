package com.edumind.backend;

import com.edumind.backend.service.StudentCsvImportService;
import com.edumind.backend.service.StudentExcelImportService;
import com.edumind.backend.service.StudentService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StudentImportPlacementTests {

    @Test
    void csvImportPassesCohortAndSectionIdsToStudentService() {
        StudentService studentService = mock(StudentService.class);
        StudentCsvImportService importService = new StudentCsvImportService(studentService);
        String csv = """
                username,password,fullName,email,rollNumber,departmentId,programId,semesterId,cohortId,sectionId
                student1,secret,Student One,student@example.com,R001,1,2,3,4,5
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.csv", "text/csv", csv.getBytes());

        var result = importService.importStudents(file);

        assertEquals(1, result.getSuccessfulRows());
        verify(studentService).createStudent(
                "student1", "secret", "Student One", "student@example.com",
                "R001", 1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void excelImportPassesCohortAndSectionIdsToStudentService() throws Exception {
        StudentService studentService = mock(StudentService.class);
        StudentExcelImportService importService = new StudentExcelImportService(studentService);

        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Students");
            var header = sheet.createRow(0);
            String[] headers = {
                    "username", "password", "fullName", "email", "rollNumber",
                    "departmentId", "programId", "semesterId", "cohortId", "sectionId"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            var row = sheet.createRow(1);
            String[] values = {
                    "student1", "secret", "Student One", "student@example.com", "R001",
                    "1", "2", "3", "4", "5"
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes);

        var result = importService.importStudents(file);

        assertEquals(1, result.getSuccessfulRows());
        verify(studentService).createStudent(
                "student1", "secret", "Student One", "student@example.com",
                "R001", 1L, 2L, 3L, 4L, 5L);
    }
}
