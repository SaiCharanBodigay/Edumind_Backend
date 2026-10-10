package com.edumind.backend.dto;

public class FacultyCourseAllocationRequest {

    private Long facultyId;
    private Long courseId;

    public FacultyCourseAllocationRequest() {
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
