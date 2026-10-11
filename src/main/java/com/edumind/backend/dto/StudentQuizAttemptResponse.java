package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StudentQuizAttemptResponse(
        Long id,
        Long quizId,
        String quizTitle,
        int attemptNumber,
        String status,
        Instant startedAt,
        Instant deadline,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        boolean resultsReleased,
        List<QuizAttemptAnswerResponse> questions) {
}
