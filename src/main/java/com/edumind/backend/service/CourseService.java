package com.edumind.backend.service;

import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.SemesterRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final FacultyCourseAllocationRepository allocationRepository;

    public CourseService(
            CourseRepository courseRepository,
            SemesterRepository semesterRepository,
            FacultyCourseAllocationRepository allocationRepository) {

        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.allocationRepository = allocationRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));
    }

    @Transactional
    public Course createCourse(
            String name,
            String code,
            Integer credits,
            Long semesterId) {

        if (courseRepository.existsByCode(code)) {
            throw new RuntimeException("Course code already exists");
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() ->
                        new RuntimeException("Semester not found"));

        Course course = new Course(
                name,
                code,
                credits,
                semester
        );

        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(
            Long id,
            String name,
            String code,
            Integer credits,
            Long semesterId) {

        Course course = getCourseById(id);

        if (courseRepository.existsByCode(code)
                && !course.getCode().equals(code)) {
            throw new RuntimeException("Course code already exists");
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() ->
                        new RuntimeException("Semester not found"));

        course.setName(name);
        course.setCode(code);
        course.setCredits(credits);
        course.setSemester(semester);

        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {

        Course course = getCourseById(id);

        if (!allocationRepository.findByCourseId(id).isEmpty()) {
            throw new RuntimeException(
                    "Cannot delete course because faculty are assigned to it"
            );
        }

        courseRepository.delete(course);
    }
}
