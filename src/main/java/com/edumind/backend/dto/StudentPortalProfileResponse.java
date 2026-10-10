package com.edumind.backend.dto;

public class StudentPortalProfileResponse {

    private final String username;
    private final String fullName;
    private final String email;
    private final String rollNumber;
    private final String departmentName;
    private final String departmentCode;
    private final String programName;
    private final String programCode;
    private final Integer semesterNumber;
    private final String academicYear;

    public StudentPortalProfileResponse(
            String username,
            String fullName,
            String email,
            String rollNumber,
            String departmentName,
            String departmentCode,
            String programName,
            String programCode,
            Integer semesterNumber,
            String academicYear) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.rollNumber = rollNumber;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.programName = programName;
        this.programCode = programCode;
        this.semesterNumber = semesterNumber;
        this.academicYear = academicYear;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getProgramName() {
        return programName;
    }

    public String getProgramCode() {
        return programCode;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public String getAcademicYear() {
        return academicYear;
    }
}
