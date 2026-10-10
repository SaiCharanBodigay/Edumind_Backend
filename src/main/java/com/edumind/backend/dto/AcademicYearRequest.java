package com.edumind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcademicYearRequest {

    @NotBlank(message = "Academic year name is required")
    @Size(max = 20, message = "Academic year name cannot exceed 20 characters")
    private String name;

    public AcademicYearRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
