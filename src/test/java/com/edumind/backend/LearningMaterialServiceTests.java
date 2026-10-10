package com.edumind.backend;

import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.LearningMaterial;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.LearningMaterialRepository;
import com.edumind.backend.service.LearningMaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningMaterialServiceTests {

    private static final String USERNAME = "faculty-alpha";
    private static final Long FACULTY_ID = 12L;
    private static final Long COURSE_ID = 31L;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private FacultyCourseAllocationRepository allocationRepository;

    @Mock
    private LearningMaterialRepository materialRepository;

    @Mock
    private com.edumind.backend.service.StudentPortalService studentPortalService;

    @TempDir
    Path storageDirectory;

    private Faculty faculty;
    private Course course;
    private LearningMaterialService service;

    @BeforeEach
    void setUp() {
        faculty = new Faculty();
        faculty.setId(FACULTY_ID);
        course = new Course();
        course.setId(COURSE_ID);
        service = new LearningMaterialService(
                facultyRepository,
                courseRepository,
                allocationRepository,
                materialRepository,
                studentPortalService,
                storageDirectory.toString());
        lenient().when(facultyRepository.findByUserUsername(USERNAME))
                .thenReturn(Optional.of(faculty));
    }

    @Test
    void facultyCanUploadTxtToAssignedCourse() throws Exception {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(materialRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "week-1.txt", "text/plain",
                "Lecture notes".getBytes(StandardCharsets.UTF_8));

        var result = service.upload(USERNAME, COURSE_ID, file);

        assertEquals(COURSE_ID, result.getCourseId());
        assertEquals("week-1.txt", result.getFilename());
        assertEquals("text/plain", result.getContentType());
        assertEquals(13L, result.getSize());
        try (var storedFiles = Files.list(storageDirectory)) {
            assertTrue(storedFiles.findAny().isPresent());
        }
        verify(materialRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void facultyCanListMaterialsForAssignedCourse() {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(materialRepository.findAllByCourseIdOrderByUploadedAtDesc(COURSE_ID))
                .thenReturn(List.of(material("notes.txt")));

        var result = service.list(USERNAME, COURSE_ID);

        assertEquals(1, result.size());
        assertEquals(COURSE_ID, result.getFirst().getCourseId());
    }

    @Test
    void facultyCannotUploadToUnassignedCourse() {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(USERNAME, COURSE_ID, new MockMultipartFile(
                        "file", "week.txt", "text/plain", new byte[] {1})));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidPdfContentEvenWhenExtensionAndMimeAreAllowed() {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(USERNAME, COURSE_ID, new MockMultipartFile(
                        "file", "fake.pdf", "application/pdf",
                        "<html>not pdf</html>".getBytes(StandardCharsets.UTF_8))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsContentTypeThatDoesNotMatchTheFileExtension() {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(USERNAME, COURSE_ID, new MockMultipartFile(
                        "file", "notes.txt", "application/pdf",
                        "plain text".getBytes(StandardCharsets.UTF_8))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsOversizedFileBeforeWritingIt() {
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        var file = org.mockito.Mockito.mock(
                org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("large.txt");
        when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);
        when(file.isEmpty()).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(USERNAME, COURSE_ID, file));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, exception.getStatusCode());
        verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void facultyCannotAccessAnotherFacultysMaterial() {
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMetadata(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void missingMaterialReturnsNotFound() {
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMetadata(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void facultyCanDownloadOwnedMaterial() throws Exception {
        LearningMaterial material = material("stored.txt");
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.of(material));
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        Files.writeString(storageDirectory.resolve("stored.txt"), "notes");

        var download = service.getDownload(USERNAME, 7L);

        try (var content = download.resource().getInputStream()) {
            assertEquals("notes", new String(content.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void downloadRejectsMissingFile() {
        LearningMaterial material = material("missing.txt");
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.of(material));
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getDownload(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void studentCannotDownloadMaterialFromUnauthorizedCourse() {
        LearningMaterial material = material("secret.txt");
        when(materialRepository.findById(7L)).thenReturn(Optional.of(material));
        when(studentPortalService.requireStudentCourse(USERNAME, COURSE_ID))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.downloadForStudent(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void facultyCannotDownloadMaterialOwnedByAnotherFaculty() {
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getDownload(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void facultyCanDeleteOwnedMaterial() throws Exception {
        LearningMaterial material = material("existing.txt");
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.of(material));
        when(allocationRepository.existsByFacultyIdAndCourseId(FACULTY_ID, COURSE_ID))
                .thenReturn(true);
        Files.writeString(storageDirectory.resolve("existing.txt"), "notes");

        service.delete(USERNAME, 7L);

        verify(materialRepository).delete(material);
        assertTrue(Files.notExists(storageDirectory.resolve("existing.txt")));
    }

    @Test
    void cannotDeleteMaterialOwnedByAnotherFaculty() {
        when(materialRepository.findByIdAndFacultyId(7L, FACULTY_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(USERNAME, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(materialRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private LearningMaterial material(String storageName) {
        return new LearningMaterial(
                course, faculty, "notes.txt", "text/plain", 5L, storageName);
    }
}
