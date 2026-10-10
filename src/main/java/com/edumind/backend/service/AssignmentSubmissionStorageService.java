package com.edumind.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class AssignmentSubmissionStorageService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssignmentSubmissionStorageService.class);
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt", "text/plain");

    private final Path storageDirectory;
    private final long maxFileSize;

    public AssignmentSubmissionStorageService(
            @Value("${edumind.storage.assignment-submissions:${user.home}/.edumind/assignment-submissions}")
            String storageDirectory,
            @Value("${edumind.assignments.max-submission-size-bytes:10485760}")
            long maxFileSize) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        if (maxFileSize < 1) {
            throw new IllegalArgumentException(
                    "edumind.assignments.max-submission-size-bytes must be positive");
        }
        this.maxFileSize = maxFileSize;
    }

    public StoredFile store(MultipartFile upload) {
        String filename = validateFilename(upload.getOriginalFilename());
        if (upload.isEmpty()) {
            throw badRequest("Choose a non-empty submission file.");
        }
        if (upload.getSize() > maxFileSize) {
            throw tooLarge();
        }

        String extension = extension(filename);
        String expectedContentType = CONTENT_TYPES.get(extension);
        if (expectedContentType == null) {
            throw badRequest("Only PDF, DOCX, PPTX, and TXT submissions are supported.");
        }
        validateContentType(upload.getContentType(), expectedContentType);

        Path staged = null;
        Path stored = null;
        try {
            Files.createDirectories(storageDirectory);
            Path realStorageDirectory = storageDirectory.toRealPath();
            staged = Files.createTempFile(realStorageDirectory, "submission-", ".tmp");
            upload.transferTo(staged);
            long size = Files.size(staged);
            if (size > maxFileSize) {
                throw tooLarge();
            }
            if (size == 0 || size != upload.getSize()) {
                throw badRequest("The uploaded file size could not be verified.");
            }
            validateContent(staged, extension);

            String storageName = UUID.randomUUID() + "." + extension;
            stored = resolve(realStorageDirectory, storageName);
            move(staged, stored);
            return new StoredFile(filename, expectedContentType, size, storageName);
        } catch (ResponseStatusException exception) {
            deleteQuietly(staged);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(staged);
            deleteQuietly(stored);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The assignment submission could not be stored.", exception);
        }
    }

    public Resource open(String storageName) {
        Path path = resolve(storageDirectory, storageName);
        try {
            if (!Files.isRegularFile(path)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "The submitted file is missing.");
            }
            return new InputStreamResource(Files.newInputStream(path));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The submitted file could not be read.", exception);
        }
    }

    public void delete(String storageName) {
        deleteQuietly(resolve(storageDirectory, storageName));
    }

    public long maxFileSize() {
        return maxFileSize;
    }

    private String validateFilename(String filename) {
        if (filename == null || filename.isBlank() || filename.length() > 255
                || filename.equals(".") || filename.equals("..")
                || filename.endsWith(".") || filename.endsWith(" ")
                || filename.contains("/") || filename.contains("\\")
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw badRequest("The submission filename is invalid.");
        }
        return filename;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateContentType(String supplied, String expected) {
        if (supplied == null) {
            throw badRequest("The submission content type is required.");
        }
        String actual = supplied.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!actual.equals(expected)
                && !actual.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            throw badRequest("The selected file's content type does not match its format.");
        }
    }

    private void validateContent(Path file, String extension) throws IOException {
        switch (extension) {
            case "pdf" -> {
                try (InputStream input = Files.newInputStream(file)) {
                    if (!startsWith(input.readNBytes(5), new byte[] {'%', 'P', 'D', 'F', '-'})) {
                        throw badRequest("The submitted file is not a valid PDF.");
                    }
                }
            }
            case "docx" -> validateOfficeArchive(file, "word/document.xml");
            case "pptx" -> validateOfficeArchive(file, "ppt/presentation.xml");
            case "txt" -> validateTextFile(file);
            default -> throw badRequest("Unsupported submission file type.");
        }
    }

    private void validateOfficeArchive(Path file, String requiredEntry)
            throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            boolean hasContentTypes = false;
            boolean hasRequiredEntry = false;
            var entries = zip.entries();
            int entryCount = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("\\")
                        || List.of(name.split("/")).contains("..")) {
                    throw badRequest("The Office document archive is invalid.");
                }
                hasContentTypes |= name.equals("[Content_Types].xml");
                hasRequiredEntry |= name.equals(requiredEntry);
                if (++entryCount > 10_000) {
                    throw badRequest("The Office document contains too many entries.");
                }
            }
            if (!hasContentTypes || !hasRequiredEntry) {
                throw badRequest("The submitted Office document is incomplete or invalid.");
            }
        } catch (java.util.zip.ZipException exception) {
            throw badRequest("The submitted file is not a valid Office document.");
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
            throw badRequest("TXT submissions must contain valid UTF-8 text.");
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

    private Path resolve(Path root, String storageName) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path fileName = Path.of(storageName).getFileName();
        Path path = normalizedRoot.resolve(storageName).normalize();
        if (fileName == null || !fileName.toString().equals(storageName)
                || !path.startsWith(normalizedRoot)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid stored submission reference.");
        }
        return path;
    }

    private void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.error("Could not clean up an assignment submission file.", exception);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException tooLarge() {
        return new ResponseStatusException(
                HttpStatus.CONTENT_TOO_LARGE,
                "Assignment submissions must be "
                        + (maxFileSize / 1024 / 1024) + " MB or smaller.");
    }

    public record StoredFile(
            String originalFilename,
            String contentType,
            Long size,
            String storageName) {
    }
}
