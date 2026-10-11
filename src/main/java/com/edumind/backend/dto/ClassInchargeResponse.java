package com.edumind.backend.dto;

import java.time.LocalDate;

public record ClassInchargeResponse(
        Long id,
        Long facultyId,
        String facultyName,
        String employeeId,
        Long sectionId,
        String sectionName,
        Long cohortId,
        String cohortName,
        Long programId,
        String programName,
        Long departmentId,
        String departmentName,
        Integer semesterNumber,
        String academicYear,
        LocalDate startDate,
        LocalDate endDate,
        boolean active) {
}
