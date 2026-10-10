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
@Table(
        name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_submission_student",
                columnNames = {"assignment_id", "student_id"}))
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CourseAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, unique = true, length = 80)
    private String storageName;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal marks;

    @Column(length = 5000)
    private String feedback;

    @Column(nullable = false)
    private boolean gradeReleased;

    @Column
    private Instant gradedAt;

    protected AssignmentSubmission() {
    }

    public AssignmentSubmission(
            CourseAssignment assignment,
            Student student,
            String originalFilename,
            String contentType,
            Long fileSize,
            String storageName) {
        this.assignment = assignment;
        this.student = student;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageName = storageName;
        this.submittedAt = Instant.now();
        this.gradeReleased = false;
    }

    public void grade(BigDecimal marks, String feedback, boolean release) {
        this.marks = marks;
        this.feedback = feedback;
        this.gradeReleased = release;
        this.gradedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CourseAssignment getAssignment() {
        return assignment;
    }

    public Student getStudent() {
        return student;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getStorageName() {
        return storageName;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public String getFeedback() {
        return feedback;
    }

    public boolean isGradeReleased() {
        return gradeReleased;
    }

    public Instant getGradedAt() {
        return gradedAt;
    }
}
