package com.edumind.backend.dto;

public class StudentResponse {

    private Long id;
    private String rollNumber;

    private String username;
    private String fullName;
    private String email;

    private Long departmentId;
    private String departmentName;
    private String departmentCode;

    private Long programId;
    private String programName;
    private String programCode;

    private Long semesterId;
    private Integer semesterNumber;
    private String academicYear;
    private Long cohortId;
    private String cohortName;
    private Long sectionId;
    private String sectionName;

    public StudentResponse() {
    }

    public StudentResponse(
            Long id,
            String rollNumber,
            String username,
            String fullName,
            String email,
            Long departmentId,
            String departmentName,
            String departmentCode,
            Long programId,
            String programName,
            String programCode,
            Long semesterId,
            Integer semesterNumber,
            String academicYear,
            Long cohortId,
            String cohortName,
            Long sectionId,
            String sectionName) {

        this.id = id;
        this.rollNumber = rollNumber;
        this.username = username;
        this.fullName = fullName;
        this.email = email;

        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;

        this.programId = programId;
        this.programName = programName;
        this.programCode = programCode;

        this.semesterId = semesterId;
        this.semesterNumber = semesterNumber;
        this.academicYear = academicYear;
        this.cohortId = cohortId;
        this.cohortName = cohortName;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
    }

    public Long getId() {
        return id;
    }

    public String getRollNumber() {
        return rollNumber;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public Long getProgramId() {
        return programId;
    }

    public String getProgramName() {
        return programName;
    }

    public String getProgramCode() {
        return programCode;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public Long getCohortId() {
        return cohortId;
    }

    public String getCohortName() {
        return cohortName;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }
}