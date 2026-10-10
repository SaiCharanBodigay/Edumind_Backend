package com.edumind.backend.controller;

import com.edumind.backend.dto.LearningMaterialResponse;
import com.edumind.backend.service.LearningMaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class LearningMaterialController {

    private final LearningMaterialService learningMaterialService;

    public LearningMaterialController(LearningMaterialService learningMaterialService) {
        this.learningMaterialService = learningMaterialService;
    }

    @PostMapping(
            path = "/courses/{courseId}/materials",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LearningMaterialResponse> upload(
            Authentication authentication,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(learningMaterialService.upload(
                        authentication.getName(), courseId, file));
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<LearningMaterialResponse> list(
            Authentication authentication,
            @PathVariable Long courseId) {
        return learningMaterialService.list(authentication.getName(), courseId);
    }

    @GetMapping("/materials/{materialId}")
    public LearningMaterialResponse getMetadata(
            Authentication authentication,
            @PathVariable Long materialId) {
        return learningMaterialService.getMetadata(authentication.getName(), materialId);
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> download(
            Authentication authentication,
            @PathVariable Long materialId) {
        LearningMaterialService.Download download =
                learningMaterialService.getDownload(authentication.getName(), materialId);
        LearningMaterialResponse metadata = download.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .contentLength(metadata.getSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(metadata.getFilename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(download.resource());
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long materialId) {
        learningMaterialService.delete(authentication.getName(), materialId);
        return ResponseEntity.noContent().build();
    }
}
