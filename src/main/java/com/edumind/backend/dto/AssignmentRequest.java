package com.edumind.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 10000) String instructions,
        @NotNull Instant dueAt,
        @NotNull @DecimalMin("0.01") BigDecimal maxMarks) {
}
