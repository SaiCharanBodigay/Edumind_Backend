package com.edumind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SectionRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull Long cohortId,
        @NotNull Long semesterId) {
}
