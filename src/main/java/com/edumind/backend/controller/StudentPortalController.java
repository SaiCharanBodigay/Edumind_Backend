package com.edumind.backend.controller;

import com.edumind.backend.dto.LearningMaterialResponse;
import com.edumind.backend.dto.AssignmentSubmissionResponse;
import com.edumind.backend.dto.StudentAssignmentResponse;
import com.edumind.backend.service.AssignmentService;
import com.edumind.backend.dto.StudentPortalCourseResponse;
import com.edumind.backend.dto.StudentPortalProfileResponse;
import com.edumind.backend.service.LearningMaterialService;
import com.edumind.backend.service.StudentPortalService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final LearningMaterialService learningMaterialService;
    private final AssignmentService assignmentService;

    public StudentPortalController(
            StudentPortalService studentPortalService,
            LearningMaterialService learningMaterialService,
            AssignmentService assignmentService) {
        this.studentPortalService = studentPortalService;
        this.learningMaterialService = learningMaterialService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/me")
    public StudentPortalProfileResponse getMyProfile(Authentication authentication) {
        return studentPortalService.getProfile(authentication.getName());
    }

    @GetMapping("/me/courses")
    public List<StudentPortalCourseResponse> getMyCourses(
            Authentication authentication) {
        return studentPortalService.getCourses(authentication.getName());
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<LearningMaterialResponse> getCourseMaterials(
            Authentication authentication,
            @PathVariable Long courseId) {
        return studentPortalService.getCourseMaterials(
                authentication.getName(), courseId);
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> download(
            Authentication authentication,
            @PathVariable Long materialId) {
        LearningMaterialService.Download download =
                learningMaterialService.downloadForStudent(
                        authentication.getName(), materialId);
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

    @GetMapping("/assignments")
    public List<StudentAssignmentResponse> getAssignments(
            Authentication authentication) {
        return assignmentService.listForStudent(authentication.getName());
    }

    @GetMapping("/assignments/{assignmentId}")
    public StudentAssignmentResponse getAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        return assignmentService.getForStudent(
                authentication.getName(), assignmentId);
    }

    @PostMapping(
            path = "/assignments/{assignmentId}/submissions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentAssignmentResponse> submit(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(assignmentService.submit(
                        authentication.getName(), assignmentId, file));
    }

    @GetMapping("/assignments/{assignmentId}/my-submission")
    public AssignmentSubmissionResponse getMySubmission(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        return assignmentService.getOwnSubmission(
                authentication.getName(), assignmentId);
    }

    @GetMapping("/assignments/{assignmentId}/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadMySubmission(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId) {
        AssignmentService.SubmissionDownload download =
                assignmentService.getStudentSubmissionDownload(
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
