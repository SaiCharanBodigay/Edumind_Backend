package com.edumind.backend.dto;

import com.edumind.backend.entity.Student;

public final class StudentResponseMapper {

    private StudentResponseMapper() {
    }

    public static StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getRollNumber(),
                student.getUser().getUsername(),
                student.getUser().getFullName(),
                student.getUser().getEmail(),
                student.getDepartment().getId(),
                student.getDepartment().getName(),
                student.getDepartment().getCode(),
                student.getProgram().getId(),
                student.getProgram().getName(),
                student.getProgram().getCode(),
                student.getSemester().getId(),
                student.getSemester().getSemesterNumber(),
                student.getSemester().getAcademicYear().getName(),
                student.getCohort() == null ? null : student.getCohort().getId(),
                student.getCohort() == null ? null : student.getCohort().getName(),
                student.getSection() == null ? null : student.getSection().getId(),
                student.getSection() == null ? null : student.getSection().getName());
    }
}
