package com.edumind.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record QuizRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 5000) String description,
        @Size(max = 10000) String instructions,
        Instant startsAt,
        Instant endsAt,
        @Min(1) @Max(600) Integer durationMinutes,
        @Min(1) @Max(20) Integer maxAttempts,
        @NotEmpty @Size(max = 100) List<@Valid QuizQuestionRequest> questions) {
}
