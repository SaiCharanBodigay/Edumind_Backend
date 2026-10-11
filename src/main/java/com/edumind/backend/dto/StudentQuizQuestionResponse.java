package com.edumind.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentQuizQuestionResponse(
        Long id,
        String text,
        BigDecimal marks,
        int position,
        List<StudentQuizOptionResponse> options) {
}
