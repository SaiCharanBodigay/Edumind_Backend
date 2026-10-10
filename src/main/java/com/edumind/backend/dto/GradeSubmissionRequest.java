package com.edumind.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeSubmissionRequest(
        @NotNull @DecimalMin("0.00") BigDecimal marks,
        @Size(max = 5000) String feedback,
        boolean releaseGrade) {
}
