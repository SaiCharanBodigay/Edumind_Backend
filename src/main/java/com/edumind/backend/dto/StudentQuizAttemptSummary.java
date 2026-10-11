package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StudentQuizAttemptSummary(
        Long id,
        int attemptNumber,
        String status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore) {
}
