package com.edumind.backend.dto;

public class AdminDashboardResponse {

    private long students;
    private long faculty;
    private long departments;
    private long programs;
    private long courses;
    private long academicYears;
    private long semesters;
    private long facultyCourseAllocations;

    public AdminDashboardResponse(
            long students,
            long faculty,
            long departments,
            long programs,
            long courses,
            long academicYears,
            long semesters,
            long facultyCourseAllocations) {

        this.students = students;
        this.faculty = faculty;
        this.departments = departments;
        this.programs = programs;
        this.courses = courses;
        this.academicYears = academicYears;
        this.semesters = semesters;
        this.facultyCourseAllocations = facultyCourseAllocations;
    }

    public long getStudents() {
        return students;
    }

    public long getFaculty() {
        return faculty;
    }

    public long getDepartments() {
        return departments;
    }

    public long getPrograms() {
        return programs;
    }

    public long getCourses() {
        return courses;
    }

    public long getAcademicYears() {
        return academicYears;
    }

    public long getSemesters() {
        return semesters;
    }

    public long getFacultyCourseAllocations() {
        return facultyCourseAllocations;
    }
}   