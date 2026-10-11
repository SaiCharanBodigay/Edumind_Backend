package com.edumind.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uk_quiz_attempt_student_number",
        columnNames = {"quiz_id", "student_id", "attempt_number"}))
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant submittedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxScore;

    protected QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, Student student, Integer attemptNumber, BigDecimal maxScore) {
        this.quiz = quiz;
        this.student = student;
        this.attemptNumber = attemptNumber;
        this.maxScore = maxScore;
        this.startedAt = Instant.now();
    }

    public void finish(String finalStatus, BigDecimal score) {
        this.status = finalStatus;
        this.score = score;
        this.submittedAt = Instant.now();
    }

    public Instant getDeadline() {
        Instant deadline = quiz.getDurationMinutes() == null ? null
                : startedAt.plusSeconds(quiz.getDurationMinutes() * 60L);
        if (quiz.getEndsAt() != null
                && (deadline == null || quiz.getEndsAt().isBefore(deadline))) {
            deadline = quiz.getEndsAt();
        }
        return deadline;
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public Student getStudent() { return student; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public String getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public BigDecimal getScore() { return score; }
    public BigDecimal getMaxScore() { return maxScore; }
}
