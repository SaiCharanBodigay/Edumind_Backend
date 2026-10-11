package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FacultyQuizAttemptResponse(
        Long id,
        Long studentId,
        String studentName,
        String rollNumber,
        int attemptNumber,
        String status,
        Instant startedAt,
        Instant deadline,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        List<FacultyAttemptAnswerResponse> questions) {
}
