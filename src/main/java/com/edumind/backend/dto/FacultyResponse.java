package com.edumind.backend.dto;

public class FacultyResponse {

    private Long id;
    private String employeeId;
    private String designation;

    private String username;
    private String fullName;
    private String email;

    private Long departmentId;
    private String departmentName;
    private String departmentCode;

    public FacultyResponse() {
    }

    public FacultyResponse(
            Long id,
            String employeeId,
            String designation,
            String username,
            String fullName,
            String email,
            Long departmentId,
            String departmentName,
            String departmentCode
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.designation = designation;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDesignation() {
        return designation;
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
}
