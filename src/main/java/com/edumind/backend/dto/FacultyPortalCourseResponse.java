package com.edumind.backend.dto;

public class FacultyPortalCourseResponse {

    private Long id;
    private String code;
    private String name;
    private Integer credits;
    private Integer semesterNumber;
    private String programName;
    private String programCode;
    private String academicYear;

    public FacultyPortalCourseResponse(
            Long id,
            String code,
            String name,
            Integer credits,
            Integer semesterNumber,
            String programName,
            String programCode,
            String academicYear) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.semesterNumber = semesterNumber;
        this.programName = programName;
        this.programCode = programCode;
        this.academicYear = academicYear;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getCredits() {
        return credits;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public String getProgramName() {
        return programName;
    }

    public String getProgramCode() {
        return programCode;
    }

    public String getAcademicYear() {
        return academicYear;
    }
}
