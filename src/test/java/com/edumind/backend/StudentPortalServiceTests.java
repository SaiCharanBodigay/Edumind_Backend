package com.edumind.backend;

import com.edumind.backend.entity.AcademicYear;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.FacultyCourseAllocation;
import com.edumind.backend.entity.LearningMaterial;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.LearningMaterialRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.StudentPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPortalServiceTests {

    private static final String USERNAME = "student-alpha";
    private static final Long COURSE_ID = 31L;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private FacultyCourseAllocationRepository allocationRepository;

    @Mock
    private LearningMaterialRepository materialRepository;

    private Student student;
    private Course course;
    private StudentPortalService service;

    @BeforeEach
    void setUp() {
        Department department = new Department("Computer Science", "CSE");
        department.setId(2L);
        Program program = new Program("B.Tech CSE", "BTECH-CSE", department);
        program.setId(3L);
        AcademicYear year = new AcademicYear("2026-27");
        year.setId(4L);
        Semester semester = new Semester(3, program, year);
        semester.setId(5L);

        User user = new User(
                USERNAME, "unused", "STUDENT", "Student Alpha", "alpha@example.test");
        student = new Student("ST-100", user, department, program, semester);

        course = new Course("Data Structures", "DS", 4, semester);
        course.setId(COURSE_ID);

        service = new StudentPortalService(
                studentRepository,
                courseRepository,
                allocationRepository,
                materialRepository);
    }

    @Test
    void profileIsResolvedOnlyFromAuthenticatedUsername() {
        when(studentRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(student));
        var profile = service.getProfile(USERNAME);

        assertEquals(USERNAME, profile.getUsername());
        assertEquals("ST-100", profile.getRollNumber());
        assertEquals("Computer Science", profile.getDepartmentName());
        assertEquals("B.Tech CSE", profile.getProgramName());
        assertEquals(3, profile.getSemesterNumber());
        verify(studentRepository).findByUserUsername(USERNAME);
    }

    @Test
    void studentGetsCoursesForTheirCurrentSemesterAndProgram() {
        when(studentRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(student));
        when(courseRepository.findAllBySemesterIdOrderByCode(5L))
                .thenReturn(List.of(course));
        when(allocationRepository.findByCourseId(COURSE_ID))
                .thenReturn(List.of(allocation("Faculty One")));
        when(materialRepository.countByCourseId(COURSE_ID)).thenReturn(2L);

        var courses = service.getCourses(USERNAME);

        assertEquals(1, courses.size());
        assertEquals(COURSE_ID, courses.getFirst().getId());
        assertEquals(List.of("Faculty One"), courses.getFirst().getFacultyNames());
        assertEquals(2L, courses.getFirst().getLearningMaterialCount());
    }

    @Test
    void courseFromAnotherSemesterIsExcludedFromStudentCourses() {
        when(studentRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(student));
        Semester otherSemester = new Semester(
                4, course.getSemester().getProgram(), course.getSemester().getAcademicYear());
        otherSemester.setId(6L);
        Course unauthorizedCourse = new Course(
                "Other Semester Course", "OTHER", 3, otherSemester);
        unauthorizedCourse.setId(88L);
        when(courseRepository.findAllBySemesterIdOrderByCode(5L))
                .thenReturn(List.of(course, unauthorizedCourse));
        when(allocationRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(materialRepository.countByCourseId(COURSE_ID)).thenReturn(0L);

        var courses = service.getCourses(USERNAME);

        assertEquals(1, courses.size());
        assertEquals(COURSE_ID, courses.getFirst().getId());
        verify(allocationRepository, never()).findByCourseId(88L);
    }

    @Test
    void studentCanListMaterialsInAnAuthorizedCourse() {
        when(studentRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        LearningMaterial material = new LearningMaterial(
                course, new Faculty(), "lecture.pdf", "application/pdf",
                1024L, "private-generated-storage-name.pdf");
        when(materialRepository.findAllByCourseIdOrderByUploadedAtDesc(COURSE_ID))
                .thenReturn(List.of(material));

        var materials = service.getCourseMaterials(USERNAME, COURSE_ID);

        assertEquals(1, materials.size());
        assertEquals("lecture.pdf", materials.getFirst().getFilename());
        assertEquals(1024L, materials.getFirst().getSize());
    }

    @Test
    void studentCannotListMaterialsFromAnotherSemester() {
        when(studentRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(student));
        Semester otherSemester = new Semester(
                4, course.getSemester().getProgram(), course.getSemester().getAcademicYear());
        otherSemester.setId(6L);
        Course unauthorizedCourse = new Course(
                "Other Semester Course", "OTHER", 3, otherSemester);
        unauthorizedCourse.setId(88L);
        when(courseRepository.findById(88L)).thenReturn(Optional.of(unauthorizedCourse));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getCourseMaterials(USERNAME, 88L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(materialRepository, never())
                .findAllByCourseIdOrderByUploadedAtDesc(88L);
    }

    @Test
    void missingStudentProfileIsRejected() {
        when(studentRepository.findByUserUsername("unknown"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getProfile("unknown"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private FacultyCourseAllocation allocation(String facultyName) {
        User facultyUser = new User(
                "faculty-one", "unused", "FACULTY", facultyName, null);
        Faculty faculty = new Faculty("EMP-1", "Lecturer", facultyUser, null);
        return new FacultyCourseAllocation(faculty, course);
    }
}
