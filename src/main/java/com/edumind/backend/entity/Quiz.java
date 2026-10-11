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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(length = 10000)
    private String instructions;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    private Instant startsAt;
    private Instant endsAt;
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer maxAttempts;

    @Column(nullable = false)
    private boolean resultsReleased;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    protected Quiz() {
    }

    public Quiz(Course course, Faculty faculty, String title, String description,
            String instructions, Instant startsAt, Instant endsAt, Integer durationMinutes,
            Integer maxAttempts) {
        this.course = course;
        this.faculty = faculty;
        this.createdAt = Instant.now();
        this.update(title, description, instructions, startsAt, endsAt, durationMinutes, maxAttempts);
    }

    public void update(String title, String description, String instructions,
            Instant startsAt, Instant endsAt, Integer durationMinutes, Integer maxAttempts) {
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.durationMinutes = durationMinutes;
        this.maxAttempts = maxAttempts;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = "PUBLISHED";
        this.updatedAt = Instant.now();
    }

    public void unpublish() {
        this.status = "DRAFT";
        this.updatedAt = Instant.now();
    }

    public void releaseResults() {
        this.resultsReleased = true;
        this.updatedAt = Instant.now();
    }

    public void replaceQuestions(List<QuizQuestion> replacement) {
        this.questions.clear();
        for (QuizQuestion question : replacement) {
            question.setQuiz(this);
            this.questions.add(question);
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getInstructions() { return instructions; }
    public String getStatus() { return status; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public boolean isResultsReleased() { return resultsReleased; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<QuizQuestion> getQuestions() { return questions; }
}
