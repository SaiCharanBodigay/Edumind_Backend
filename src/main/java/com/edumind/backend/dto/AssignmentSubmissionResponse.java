package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentSubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String rollNumber,
        String filename,
        String contentType,
        Long size,
        Instant submittedAt,
        BigDecimal marks,
        String feedback,
        boolean gradeReleased,
        Instant gradedAt) {
}
