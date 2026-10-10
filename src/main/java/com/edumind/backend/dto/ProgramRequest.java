package com.edumind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProgramRequest {

    @NotBlank(message = "Program name is required")
    @Size(max = 100, message = "Program name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Program code is required")
    @Size(max = 20, message = "Program code cannot exceed 20 characters")
    private String code;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    public ProgramRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
