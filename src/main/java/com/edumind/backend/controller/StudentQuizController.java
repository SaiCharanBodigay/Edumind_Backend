package com.edumind.backend.controller;

import com.edumind.backend.dto.QuizAnswerRequest;
import com.edumind.backend.dto.ReleasedQuizResultResponse;
import com.edumind.backend.dto.StudentQuizAttemptResponse;
import com.edumind.backend.dto.StudentQuizResponse;
import com.edumind.backend.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentQuizController {

    private final QuizService quizService;

    public StudentQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/quizzes")
    public List<StudentQuizResponse> list(Authentication authentication) {
        return quizService.listForStudent(authentication.getName());
    }

    @GetMapping("/quizzes/{quizId}")
    public StudentQuizResponse get(
            Authentication authentication, @PathVariable Long quizId) {
        return quizService.getForStudent(authentication.getName(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<StudentQuizAttemptResponse> start(
            Authentication authentication, @PathVariable Long quizId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.startAttempt(authentication.getName(), quizId));
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public StudentQuizAttemptResponse getAttempt(
            Authentication authentication, @PathVariable Long attemptId) {
        return quizService.getStudentAttempt(authentication.getName(), attemptId);
    }

    @PutMapping("/quiz-attempts/{attemptId}/answers/{questionId}")
    public StudentQuizAttemptResponse answer(
            Authentication authentication,
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuizAnswerRequest request) {
        return quizService.saveAnswer(
                authentication.getName(), attemptId, questionId, request);
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public StudentQuizAttemptResponse submit(
            Authentication authentication, @PathVariable Long attemptId) {
        return quizService.submit(authentication.getName(), attemptId);
    }

    @GetMapping("/quizzes/{quizId}/attempts/{attemptId}/results")
    public ReleasedQuizResultResponse result(
            Authentication authentication,
            @PathVariable Long quizId,
            @PathVariable Long attemptId) {
        return quizService.getReleasedResult(
                authentication.getName(), quizId, attemptId);
    }
}
