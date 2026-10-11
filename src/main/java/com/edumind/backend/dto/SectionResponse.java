package com.edumind.backend.dto;

public record SectionResponse(
        Long id,
        String name,
        Long cohortId,
        String cohortName,
        Long programId,
        String programName,
        Long departmentId,
        String departmentName,
        Long semesterId,
        Integer semesterNumber,
        String academicYear,
        String activeInchargeName) {
}
