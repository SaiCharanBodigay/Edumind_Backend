package com.edumind.backend.dto;

import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest(@NotNull Long optionId) {
}
