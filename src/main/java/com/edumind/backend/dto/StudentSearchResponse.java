package com.edumind.backend.dto;

import java.util.List;

public record StudentSearchResponse(
        List<StudentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
