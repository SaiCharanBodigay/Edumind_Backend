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

@Entity
@Table(name = "quiz_options")
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private boolean correct;

    protected QuizOption() {
    }

    public QuizOption(String text, Integer position, boolean correct) {
        this.text = text;
        this.position = position;
        this.correct = correct;
    }

    public void setQuestion(QuizQuestion question) { this.question = question; }
    public Long getId() { return id; }
    public QuizQuestion getQuestion() { return question; }
    public String getText() { return text; }
    public Integer getPosition() { return position; }
    public boolean isCorrect() { return correct; }
}
