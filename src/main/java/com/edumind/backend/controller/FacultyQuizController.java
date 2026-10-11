package com.edumind.backend.controller;

import com.edumind.backend.dto.FacultyQuizAttemptResponse;
import com.edumind.backend.dto.QuizRequest;
import com.edumind.backend.dto.QuizResponse;
import com.edumind.backend.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyQuizController {

    private final QuizService quizService;

    public FacultyQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public List<QuizResponse> list(
            Authentication authentication, @PathVariable Long courseId) {
        return quizService.listForFaculty(authentication.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<QuizResponse> create(
            Authentication authentication,
            @PathVariable Long courseId,
            @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.create(authentication.getName(), courseId, request));
    }

    @GetMapping("/quizzes/{quizId}")
    public QuizResponse get(Authentication authentication, @PathVariable Long quizId) {
        return quizService.getForFaculty(authentication.getName(), quizId);
    }

    @PutMapping("/quizzes/{quizId}")
    public QuizResponse update(
            Authentication authentication,
            @PathVariable Long quizId,
            @Valid @RequestBody QuizRequest request) {
        return quizService.update(authentication.getName(), quizId, request);
    }

    @PostMapping("/quizzes/{quizId}/publish")
    public QuizResponse publish(Authentication authentication, @PathVariable Long quizId) {
        return quizService.publish(authentication.getName(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/unpublish")
    public QuizResponse unpublish(Authentication authentication, @PathVariable Long quizId) {
        return quizService.unpublish(authentication.getName(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/release-results")
    public QuizResponse releaseResults(Authentication authentication, @PathVariable Long quizId) {
        return quizService.releaseResults(authentication.getName(), quizId);
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long quizId) {
        quizService.delete(authentication.getName(), quizId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    public List<FacultyQuizAttemptResponse> attempts(
            Authentication authentication, @PathVariable Long quizId) {
        return quizService.listFacultyAttempts(authentication.getName(), quizId);
    }
}
