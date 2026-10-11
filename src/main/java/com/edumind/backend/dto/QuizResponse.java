package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        String title,
        String description,
        String instructions,
        String status,
        Instant startsAt,
        Instant endsAt,
        Integer durationMinutes,
        Integer maxAttempts,
        BigDecimal totalMarks,
        boolean resultsReleased,
        int questionCount,
        long attemptCount,
        long activeAttemptCount,
        boolean canReleaseResults,
        List<QuizQuestionResponse> questions) {
}
