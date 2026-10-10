package com.edumind.backend.dto;

public class FacultyCourseAllocationResponse {

    private Long id;

    private Long facultyId;
    private String facultyName;
    private String employeeId;

    private Long courseId;
    private String courseName;
    private String courseCode;

    public FacultyCourseAllocationResponse() {
    }

    public FacultyCourseAllocationResponse(
            Long id,
            Long facultyId,
            String facultyName,
            String employeeId,
            Long courseId,
            String courseName,
            String courseCode) {

        this.id = id;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.employeeId = employeeId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
    }

    public Long getId() {
        return id;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }
}
