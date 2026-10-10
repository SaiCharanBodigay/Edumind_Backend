package com.edumind.backend.service;

import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.FacultyCourseAllocation;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacultyCourseAllocationService {

    private final FacultyCourseAllocationRepository allocationRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;

    public FacultyCourseAllocationService(
            FacultyCourseAllocationRepository allocationRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository) {

        this.allocationRepository = allocationRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
    }

    public List<FacultyCourseAllocation> getAllAllocations() {
        return allocationRepository.findAll();
    }

    public FacultyCourseAllocation getAllocationById(Long id) {
        return allocationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Faculty course allocation not found with id: " + id));
    }

    @Transactional
    public FacultyCourseAllocation createAllocation(
            Long facultyId,
            Long courseId) {

        // Find faculty
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() ->
                        new RuntimeException("Faculty not found"));

        // Find course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));

        // Prevent duplicate allocation
        if (allocationRepository
                .existsByFacultyIdAndCourseId(facultyId, courseId)) {

            throw new RuntimeException(
                    "This faculty member is already assigned to this course");
        }

        /*
         * Faculty and course should belong to the
         * same department.
         *
         * Faculty:
         *     faculty.department
         *
         * Course:
         *     course.semester.program.department
         */

        Long facultyDepartmentId =
                faculty.getDepartment().getId();

        Long courseDepartmentId =
                course.getSemester()
                      .getProgram()
                      .getDepartment()
                      .getId();

        if (!facultyDepartmentId.equals(courseDepartmentId)) {
            throw new RuntimeException(
                    "Faculty and course belong to different departments");
        }

        // Create allocation
        FacultyCourseAllocation allocation =
                new FacultyCourseAllocation();

        allocation.setFaculty(faculty);
        allocation.setCourse(course);

        return allocationRepository.save(allocation);
    }

    public void deleteAllocation(Long id) {

        if (!allocationRepository.existsById(id)) {
            throw new RuntimeException(
                    "Faculty course allocation not found");
        }

        allocationRepository.deleteById(id);
    }
}
