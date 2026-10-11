package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StudentQuizResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        String title,
        String description,
        String instructions,
        Instant startsAt,
        Instant endsAt,
        Integer durationMinutes,
        Integer maxAttempts,
        BigDecimal totalMarks,
        String availability,
        int attemptsUsed,
        Long activeAttemptId,
        boolean resultsReleased,
        List<StudentQuizAttemptSummary> attempts,
        List<StudentQuizQuestionResponse> questions) {
}
