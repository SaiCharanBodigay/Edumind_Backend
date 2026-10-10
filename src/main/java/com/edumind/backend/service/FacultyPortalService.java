package com.edumind.backend.service;

import com.edumind.backend.dto.FacultyPortalCourseResponse;
import com.edumind.backend.dto.FacultyPortalProfileResponse;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.FacultyCourseAllocation;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyPortalService {

    private final FacultyRepository facultyRepository;
    private final FacultyCourseAllocationRepository allocationRepository;

    public FacultyPortalService(
            FacultyRepository facultyRepository,
            FacultyCourseAllocationRepository allocationRepository) {
        this.facultyRepository = facultyRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional(readOnly = true)
    public FacultyPortalProfileResponse getProfile(String username) {
        Faculty faculty = findFaculty(username);

        return new FacultyPortalProfileResponse(
                faculty.getId(),
                faculty.getEmployeeId(),
                faculty.getDesignation(),
                faculty.getUser().getUsername(),
                faculty.getUser().getFullName(),
                faculty.getUser().getEmail(),
                faculty.getDepartment().getName(),
                faculty.getDepartment().getCode()
        );
    }

    @Transactional(readOnly = true)
    public List<FacultyPortalCourseResponse> getAssignedCourses(String username) {
        findFaculty(username);

        return allocationRepository.findPortalCoursesByFacultyUsername(username)
                .stream()
                .map(this::toCourseResponse)
                .toList();
    }

    private Faculty findFaculty(String username) {
        return facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Faculty profile not found for the authenticated user"));
    }

    private FacultyPortalCourseResponse toCourseResponse(
            FacultyCourseAllocation allocation) {
        Course course = allocation.getCourse();
        Semester semester = course.getSemester();

        return new FacultyPortalCourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getCredits(),
                semester.getSemesterNumber(),
                semester.getProgram().getName(),
                semester.getProgram().getCode(),
                semester.getAcademicYear().getName()
        );
    }
}
