package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuizAttemptAnswerResponse(
        Long questionId,
        String questionText,
        BigDecimal marks,
        Long selectedOptionId,
        List<StudentQuizOptionResponse> options) {
}
