package com.edumind.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ClassInchargeRequest(
        @NotNull Long facultyId,
        @NotNull Long sectionId,
        LocalDate startDate) {
}
