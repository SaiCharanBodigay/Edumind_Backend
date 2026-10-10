package com.edumind.backend.dto;

public class FacultyPortalProfileResponse {

    private Long id;
    private String employeeId;
    private String designation;
    private String username;
    private String fullName;
    private String email;
    private String departmentName;
    private String departmentCode;

    public FacultyPortalProfileResponse(
            Long id,
            String employeeId,
            String designation,
            String username,
            String fullName,
            String email,
            String departmentName,
            String departmentCode) {
        this.id = id;
        this.employeeId = employeeId;
        this.designation = designation;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }
}
