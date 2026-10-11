package com.edumind.backend.dto;

import java.math.BigDecimal;

public record ReleasedQuizAnswerResponse(
        Long questionId,
        String questionText,
        BigDecimal marks,
        Long selectedOptionId,
        String selectedOptionText,
        Long correctOptionId,
        String correctOptionText,
        boolean correct) {
}
