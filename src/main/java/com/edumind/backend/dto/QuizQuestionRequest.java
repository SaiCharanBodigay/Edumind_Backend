package com.edumind.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record QuizQuestionRequest(
        @NotBlank @Size(max = 4000) String text,
        @DecimalMin("0.01") BigDecimal marks,
        @NotEmpty @Size(max = 6) List<@Valid QuizOptionRequest> options) {
}
