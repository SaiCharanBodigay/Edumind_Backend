package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        String title,
        String description,
        String instructions,
        Instant dueAt,
        BigDecimal maxMarks,
        Instant createdAt,
        Instant updatedAt,
        Long submissionCount) {
}
