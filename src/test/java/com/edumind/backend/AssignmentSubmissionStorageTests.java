package com.edumind.backend;

import com.edumind.backend.service.AssignmentSubmissionStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentSubmissionStorageTests {

    @TempDir
    Path tempDirectory;

    private AssignmentSubmissionStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new AssignmentSubmissionStorageService(
                tempDirectory.toString(), 8L);
    }

    @Test
    void storesTxtUnderGeneratedNameAndStreamsContents() throws Exception {
        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "student-work.txt",
                "text/plain",
                "12345678".getBytes(StandardCharsets.UTF_8));

        var stored = storageService.store(upload);

        assertEquals("student-work.txt", stored.originalFilename());
        assertEquals(8L, stored.size());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                stored.originalFilename(), stored.storageName());
        try (var content = storageService.open(stored.storageName()).getInputStream()) {
            assertEquals("12345678", new String(
                    content.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsInvalidContentDespiteSupportedExtension() {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "bad".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> storageService.store(upload));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectsOversizedSubmission() {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "work.txt", "text/plain", "123456789".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> storageService.store(upload));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, exception.getStatusCode());
    }

    @Test
    void rejectsPathTraversalFilename() {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "..\\escape.txt", "text/plain", "work".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> storageService.store(upload));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        try (var files = Files.list(tempDirectory)) {
            assertEquals(0L, files.count());
        } catch (java.io.IOException exceptionFromList) {
            throw new AssertionError(exceptionFromList);
        }
    }
}
