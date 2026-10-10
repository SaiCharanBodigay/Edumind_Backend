package com.edumind.backend.controller;

import com.edumind.backend.dto.AssignmentRequest;
import com.edumind.backend.dto.AssignmentResponse;
import com.edumind.backend.dto.AssignmentSubmissionResponse;
import com.edumind.backend.dto.GradeSubmissionRequest;
import com.edumind.backend.service.AssignmentService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyAssignmentController {

    private final AssignmentService assignmentService;

    public FacultyAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/courses/{courseId}/assignments")
    public List<AssignmentResponse> getCourseAssignments(
            Authentication authentication,
            @PathVariable Long courseId) {
        return assignmentService.listForFaculty(authentication.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/assignments")
    public ResponseEntity<AssignmentResponse> create(
            Authentication authentication,
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.create(
                        authentication.getName(), courseId, request));
    }

    @GetMapping("/assignments/{assignmentId}")
    public AssignmentResponse get(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        return assignmentService.getForFaculty(
                authentication.getName(), assignmentId);
    }

    @PutMapping("/assignments/{assignmentId}")
    public AssignmentResponse update(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(
                authentication.getName(), assignmentId, request);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        assignmentService.delete(authentication.getName(), assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public List<AssignmentSubmissionResponse> getSubmissions(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        return assignmentService.listSubmissions(
                authentication.getName(), assignmentId);
    }

    @PutMapping("/assignments/{assignmentId}/submissions/{submissionId}/grade")
    public AssignmentSubmissionResponse grade(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        return assignmentService.grade(
                authentication.getName(), assignmentId, submissionId, request);
    }

    @GetMapping("/assignments/{assignmentId}/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadSubmission(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId) {
        AssignmentService.SubmissionDownload download =
                assignmentService.getFacultySubmissionDownload(
                        authentication.getName(), assignmentId, submissionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(download.resource());
    }
}
