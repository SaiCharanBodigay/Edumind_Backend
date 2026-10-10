package com.edumind.backend.dto;

import java.util.List;

public class StudentPortalCourseResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final Integer credits;
    private final Integer semesterNumber;
    private final String programName;
    private final String academicYear;
    private final List<String> facultyNames;
    private final long learningMaterialCount;

    public StudentPortalCourseResponse(
            Long id,
            String code,
            String name,
            Integer credits,
            Integer semesterNumber,
            String programName,
            String academicYear,
            List<String> facultyNames,
            long learningMaterialCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.semesterNumber = semesterNumber;
        this.programName = programName;
        this.academicYear = academicYear;
        this.facultyNames = List.copyOf(facultyNames);
        this.learningMaterialCount = learningMaterialCount;
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

    public String getAcademicYear() {
        return academicYear;
    }

    public List<String> getFacultyNames() {
        return facultyNames;
    }

    public long getLearningMaterialCount() {
        return learningMaterialCount;
    }
}
