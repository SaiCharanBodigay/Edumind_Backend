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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "course_assignments")
public class CourseAssignment {

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

    @Column(nullable = false, length = 10000)
    private String instructions;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxMarks;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CourseAssignment() {
    }

    public CourseAssignment(
            Course course,
            Faculty faculty,
            String title,
            String description,
            String instructions,
            Instant dueAt,
            BigDecimal maxMarks) {
        this.course = course;
        this.faculty = faculty;
        update(title, description, instructions, dueAt, maxMarks);
        this.createdAt = Instant.now();
    }

    public void update(
            String title,
            String description,
            String instructions,
            Instant dueAt,
            BigDecimal maxMarks) {
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.dueAt = dueAt;
        this.maxMarks = maxMarks;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
