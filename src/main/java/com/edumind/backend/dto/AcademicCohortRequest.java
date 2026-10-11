package com.edumind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcademicCohortRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull Long programId) {
}
