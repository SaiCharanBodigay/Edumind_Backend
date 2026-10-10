package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StudentAssignmentResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        String title,
        String description,
        String instructions,
        Instant dueAt,
        BigDecimal maxMarks,
        String submissionStatus,
        Long submissionId,
        String submittedFilename,
        Instant submittedAt,
        BigDecimal releasedMarks,
        String releasedFeedback,
        Instant releasedAt) {
}
