package com.edumind.backend.service;

import com.edumind.backend.dto.LearningMaterialResponse;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.LearningMaterial;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.LearningMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class LearningMaterialService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LearningMaterialService.class);
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 10_000;
    private static final Map<String, AllowedFile> ALLOWED_FILES = Map.of(
            "pdf", new AllowedFile("application/pdf"),
            "docx", new AllowedFile(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "pptx", new AllowedFile(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            "txt", new AllowedFile("text/plain")
    );

    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseAllocationRepository allocationRepository;
    private final LearningMaterialRepository materialRepository;
    private final StudentPortalService studentPortalService;
    private final Path storageDirectory;

    public LearningMaterialService(
            FacultyRepository facultyRepository,
            CourseRepository courseRepository,
            FacultyCourseAllocationRepository allocationRepository,
            LearningMaterialRepository materialRepository,
            StudentPortalService studentPortalService,
            @Value("${edumind.storage.learning-materials:./data/learning-materials}")
            String storageDirectory) {
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.materialRepository = materialRepository;
        this.studentPortalService = studentPortalService;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public LearningMaterialResponse upload(
            String username,
            Long courseId,
            MultipartFile file) {
        Faculty faculty = requireFaculty(username);
        Course course = requireAssignedCourse(faculty, courseId);
        String filename = validateFilename(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw badRequest("Choose a non-empty file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.CONTENT_TOO_LARGE, "Files must be 20 MB or smaller.");
        }

        String extension = extension(filename);
        AllowedFile allowedFile = ALLOWED_FILES.get(extension);
        if (allowedFile == null) {
            throw badRequest("Only PDF, DOCX, PPTX, and TXT files are supported.");
        }
        String contentType = validateContentType(file.getContentType(), allowedFile);

        Path stagedFile = null;
        Path storedFile = null;
        try {
            Files.createDirectories(storageDirectory);
            Path realStorageDirectory = storageDirectory.toRealPath();
            stagedFile = Files.createTempFile(realStorageDirectory, "upload-", ".tmp");
            file.transferTo(stagedFile);
            long storedSize = Files.size(stagedFile);
            if (storedSize > MAX_FILE_SIZE) {
                throw new ResponseStatusException(
                        HttpStatus.CONTENT_TOO_LARGE, "Files must be 20 MB or smaller.");
            }
            if (storedSize != file.getSize()) {
                throw badRequest("The uploaded file size could not be verified.");
            }
            validateContent(stagedFile, extension);

            String storageName = UUID.randomUUID() + "." + extension;
            storedFile = resolveStoragePath(realStorageDirectory, storageName);
            moveSafely(stagedFile, storedFile);

            LearningMaterial material = new LearningMaterial(
                    course, faculty, filename, contentType, file.getSize(), storageName);
            LearningMaterial saved = materialRepository.save(material);
            removeUploadedFileIfTransactionRollsBack(storedFile);
            return toResponse(saved);
        } catch (ResponseStatusException exception) {
            deleteQuietly(stagedFile);
            throw exception;
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(stagedFile);
            deleteQuietly(storedFile);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The learning material could not be stored.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> list(String username, Long courseId) {
        Faculty faculty = requireFaculty(username);
        requireAssignedCourse(faculty, courseId);
        return materialRepository.findAllByCourseIdOrderByUploadedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningMaterialResponse getMetadata(String username, Long materialId) {
        return toResponse(requireOwnedMaterial(username, materialId));
    }

    @Transactional(readOnly = true)
    public Download getDownload(String username, Long materialId) {
        LearningMaterial material = requireOwnedMaterial(username, materialId);
        return getDownload(material);
    }

    @Transactional(readOnly = true)
    public Download downloadForStudent(String username, Long materialId) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Learning material not found."));
        studentPortalService.requireStudentCourse(
                username, material.getCourse().getId());
        return getDownload(material);
    }

    private Download getDownload(LearningMaterial material) {
        Path path = resolveStoragePath(storageDirectory, material.getStorageName());
        try {
            if (!Files.isRegularFile(path)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "The learning material file is missing.");
            }
            return new Download(
                    toResponse(material), new InputStreamResource(Files.newInputStream(path)));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The learning material could not be read.", exception);
        }
    }

    @Transactional
    public void delete(String username, Long materialId) {
        LearningMaterial material = requireOwnedMaterial(username, materialId);
        Path path = resolveStoragePath(storageDirectory, material.getStorageName());
        Path quarantinedFile = null;
        try {
            if (Files.exists(path)) {
                quarantinedFile = resolveStoragePath(
                        storageDirectory, UUID.randomUUID() + ".deleting");
                moveSafely(path, quarantinedFile);
            }
            materialRepository.delete(material);
            removeDeletedFileAfterTransaction(path, quarantinedFile);
        } catch (IOException exception) {
            restoreQuarantinedFile(quarantinedFile, path);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The learning material could not be deleted.", exception);
        } catch (RuntimeException exception) {
            restoreQuarantinedFile(quarantinedFile, path);
            throw exception;
        }
    }

    private void removeUploadedFileIfTransactionRollsBack(Path storedFile) {
        if (storedFile != null
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                deleteQuietly(storedFile);
                            }
                        }
                    });
        }
    }

    private void removeDeletedFileAfterTransaction(Path original, Path quarantined) {
        if (quarantined == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(quarantined);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            deleteQuietly(quarantined);
                        } else {
                            restoreQuarantinedFile(quarantined, original);
                        }
                    }
                });
    }

    private void restoreQuarantinedFile(Path quarantined, Path original) {
        if (quarantined == null || !Files.exists(quarantined)) {
            return;
        }
        try {
            moveSafely(quarantined, original);
        } catch (IOException exception) {
            LOGGER.error("Could not restore a learning material after a failed deletion.", exception);
        }
    }

    private Faculty requireFaculty(String username) {
        return facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Faculty profile not found."));
    }

    private Course requireAssignedCourse(Faculty faculty, Long courseId) {
        if (!allocationRepository.existsByFacultyIdAndCourseId(faculty.getId(), courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Assigned course not found.");
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assigned course not found."));
    }

    private LearningMaterial requireOwnedMaterial(String username, Long materialId) {
        Faculty faculty = requireFaculty(username);
        LearningMaterial material = materialRepository
                .findByIdAndFacultyId(materialId, faculty.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Learning material not found."));
        if (!allocationRepository.existsByFacultyIdAndCourseId(
                faculty.getId(), material.getCourse().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Learning material not found.");
        }
        return material;
    }

    private LearningMaterialResponse toResponse(LearningMaterial material) {
        return new LearningMaterialResponse(
                material.getId(),
                material.getCourse().getId(),
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSize(),
                material.getUploadedAt());
    }

    private String validateFilename(String filename) {
        if (filename == null || filename.isBlank() || filename.length() > 255
                || filename.equals(".") || filename.equals("..")
                || filename.endsWith(".") || filename.endsWith(" ")
                || filename.contains("/") || filename.contains("\\")
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw badRequest("The filename is invalid.");
        }
        return filename;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String validateContentType(String suppliedType, AllowedFile allowedFile) {
        if (suppliedType == null) {
            throw badRequest("The file content type is required.");
        }
        String normalized = suppliedType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(allowedFile.contentType)
                && !normalized.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            throw badRequest("The selected file type does not match its content.");
        }
        return allowedFile.contentType;
    }

    private void validateContent(Path file, String extension) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] signature = input.readNBytes(8);
            switch (extension) {
                case "pdf" -> {
                    if (!startsWith(signature, new byte[] {'%', 'P', 'D', 'F', '-'})) {
                        throw badRequest("The uploaded file is not a valid PDF.");
                    }
                }
                case "docx" -> validateOfficeArchive(file, "word/document.xml");
                case "pptx" -> validateOfficeArchive(file, "ppt/presentation.xml");
                case "txt" -> validateTextFile(file);
                default -> throw badRequest("Unsupported file type.");
            }
        }
    }

    private void validateOfficeArchive(Path file, String requiredEntry)
            throws IOException {
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            List<String> entryNames = new ArrayList<>();
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\")
                        || List.of(name.split("/")).contains("..")) {
                    throw badRequest("The Office document archive is invalid.");
                }
                entryNames.add(name);
                if (entryNames.size() > MAX_ZIP_ENTRIES) {
                    throw badRequest("The Office document contains too many entries.");
                }
            }
            if (!entryNames.contains("[Content_Types].xml")
                    || !entryNames.contains(requiredEntry)) {
                throw badRequest("The uploaded Office document is incomplete or invalid.");
            }
        } catch (java.util.zip.ZipException exception) {
            throw badRequest("The uploaded file is not a valid Office document.");
        }
    }

    private void validateTextFile(Path file) throws IOException {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(file), decoder)) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                for (int index = 0; index < count; index++) {
                    char value = buffer[index];
                    if (value == '\0' || (Character.isISOControl(value)
                            && value != '\t' && value != '\n'
                            && value != '\r' && value != '\f')) {
                        throw badRequest("The TXT file contains unsupported characters.");
                    }
                }
            }
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw badRequest("TXT files must contain valid UTF-8 text.");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private Path resolveStoragePath(Path root, String storageName) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path path = normalizedRoot.resolve(storageName).normalize();
        if (!path.startsWith(normalizedRoot) || !Path.of(storageName).getFileName()
                .toString().equals(storageName)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid stored file reference.");
        }
        return path;
    }

    private void moveSafely(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                LOGGER.error("Could not clean up a learning material storage file.", exception);
            }
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record AllowedFile(String contentType) {
    }

    public record Download(LearningMaterialResponse metadata, Resource resource) {
    }
}
