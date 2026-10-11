package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReleasedQuizResultResponse(
        Long attemptId,
        String status,
        BigDecimal score,
        BigDecimal maxScore,
        List<ReleasedQuizAnswerResponse> answers) {
}
