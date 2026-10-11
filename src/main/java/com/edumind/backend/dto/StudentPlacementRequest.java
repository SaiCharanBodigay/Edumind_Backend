package com.edumind.backend.dto;

import jakarta.validation.constraints.NotNull;

public record StudentPlacementRequest(
        @NotNull Long cohortId,
        @NotNull Long semesterId,
        @NotNull Long sectionId) {
}
