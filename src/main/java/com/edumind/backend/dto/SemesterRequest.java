package com.edumind.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SemesterRequest {

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester number must be between 1 and 8")
    @Max(value = 8, message = "Semester number must be between 1 and 8")
    private Integer semesterNumber;

    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    public SemesterRequest() {
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }
}