package com.edumind.backend.service;

import com.edumind.backend.dto.LearningMaterialResponse;
import com.edumind.backend.dto.StudentPortalCourseResponse;
import com.edumind.backend.dto.StudentPortalProfileResponse;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.FacultyCourseAllocation;
import com.edumind.backend.entity.LearningMaterial;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.entity.Student;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.LearningMaterialRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StudentPortalService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseAllocationRepository allocationRepository;
    private final LearningMaterialRepository materialRepository;

    public StudentPortalService(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            FacultyCourseAllocationRepository allocationRepository,
            LearningMaterialRepository materialRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional(readOnly = true)
    public StudentPortalProfileResponse getProfile(String username) {
        Student student = requireStudent(username);
        Semester semester = student.getSemester();
        return new StudentPortalProfileResponse(
                student.getUser().getUsername(),
                student.getUser().getFullName(),
                student.getUser().getEmail(),
                student.getRollNumber(),
                student.getDepartment().getName(),
                student.getDepartment().getCode(),
                student.getProgram().getName(),
                student.getProgram().getCode(),
                semester.getSemesterNumber(),
                semester.getAcademicYear().getName(),
                student.getCohort() == null ? null : student.getCohort().getName(),
                student.getSection() == null ? null : student.getSection().getName());
    }

    @Transactional(readOnly = true)
    public List<StudentPortalCourseResponse> getCourses(String username) {
        Student student = requireStudent(username);
        return courseRepository
                .findAllBySemesterIdOrderByCode(student.getSemester().getId())
                .stream()
                .filter(course -> isCourseInStudentProgram(student, course))
                .map(this::toCourseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> getCourseMaterials(
            String username,
            Long courseId) {
        Student student = requireStudent(username);
        Course course = requireAuthorizedCourse(student, courseId);
        return materialRepository.findAllByCourseIdOrderByUploadedAtDesc(course.getId())
                .stream()
                .map(this::toMaterialResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Course requireStudentCourse(String username, Long courseId) {
        return requireAuthorizedCourse(requireStudent(username), courseId);
    }

    private Student requireStudent(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student profile not found."));
    }

    private Course requireAuthorizedCourse(Student student, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .filter(candidate -> isCourseInStudentProgram(student, candidate))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."));
        return course;
    }

    private boolean isCourseInStudentProgram(Student student, Course course) {
        return course.getSemester().getId().equals(student.getSemester().getId())
                && course.getSemester().getProgram().getId()
                        .equals(student.getProgram().getId());
    }

    private StudentPortalCourseResponse toCourseResponse(Course course) {
        Semester semester = course.getSemester();
        List<String> facultyNames = allocationRepository.findByCourseId(course.getId())
                .stream()
                .map(FacultyCourseAllocation::getFaculty)
                .map(faculty -> faculty.getUser().getFullName())
                .distinct()
                .toList();

        return new StudentPortalCourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getCredits(),
                semester.getSemesterNumber(),
                semester.getProgram().getName(),
                semester.getAcademicYear().getName(),
                facultyNames,
                materialRepository.countByCourseId(course.getId()));
    }

    private LearningMaterialResponse toMaterialResponse(LearningMaterial material) {
        return new LearningMaterialResponse(
                material.getId(),
                material.getCourse().getId(),
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSize(),
                material.getUploadedAt());
    }
}
