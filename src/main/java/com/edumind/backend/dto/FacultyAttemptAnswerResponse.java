package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record FacultyAttemptAnswerResponse(
        Long questionId,
        String questionText,
        BigDecimal marks,
        Long selectedOptionId,
        Long correctOptionId,
        Boolean correct,
        List<QuizOptionResponse> options) {
}
