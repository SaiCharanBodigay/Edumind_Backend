package com.edumind.backend.dto;

public record AcademicCohortResponse(
        Long id,
        String name,
        Long programId,
        String programName,
        Long departmentId,
        String departmentName) {
}
