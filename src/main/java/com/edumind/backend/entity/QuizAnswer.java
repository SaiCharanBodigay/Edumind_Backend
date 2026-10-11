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

@Entity
@Table(name = "quiz_answers", uniqueConstraints = @UniqueConstraint(
        name = "uk_quiz_answer_attempt_question",
        columnNames = {"attempt_id", "question_id"}))
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_option_id", nullable = false)
    private QuizOption selectedOption;

    @Column(nullable = false)
    private boolean correct;

    protected QuizAnswer() {
    }

    public QuizAnswer(QuizAttempt attempt, QuizQuestion question, QuizOption selectedOption) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = selectedOption.isCorrect();
    }

    public void select(QuizOption option) {
        this.selectedOption = option;
        this.correct = option.isCorrect();
    }

    public Long getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public QuizQuestion getQuestion() { return question; }
    public QuizOption getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
}
