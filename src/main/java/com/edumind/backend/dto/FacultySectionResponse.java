package com.edumind.backend.dto;

public record FacultySectionResponse(
        Long sectionId,
        String sectionName,
        Long cohortId,
        String cohortName,
        String programName,
        String departmentName,
        Integer semesterNumber,
        String academicYear) {
}
