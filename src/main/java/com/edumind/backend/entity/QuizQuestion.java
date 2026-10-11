package com.edumind.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marks;

    @Column(nullable = false)
    private Integer position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizOption> options = new ArrayList<>();

    protected QuizQuestion() {
    }

    public QuizQuestion(String text, BigDecimal marks, Integer position) {
        this.text = text;
        this.marks = marks;
        this.position = position;
    }

    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public void replaceOptions(List<QuizOption> replacement) {
        this.options.clear();
        for (QuizOption option : replacement) {
            option.setQuestion(this);
            this.options.add(option);
        }
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getText() { return text; }
    public BigDecimal getMarks() { return marks; }
    public Integer getPosition() { return position; }
    public List<QuizOption> getOptions() { return options; }
}
