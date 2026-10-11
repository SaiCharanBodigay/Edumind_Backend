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

import java.time.Instant;

@Entity
@Table(
        name = "student_academic_placements",
        indexes = {
                @jakarta.persistence.Index(
                        name = "idx_student_placement_current",
                        columnList = "student_id,ended_at"),
                @jakarta.persistence.Index(
                        name = "idx_student_placement_section",
                        columnList = "section_id,ended_at")
        })
public class StudentAcademicPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private AcademicCohort cohort;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant endedAt;

    protected StudentAcademicPlacement() {
    }

    public StudentAcademicPlacement(
            Student student,
            AcademicCohort cohort,
            Semester semester,
            Section section) {
        this.student = student;
        this.cohort = cohort;
        this.semester = semester;
        this.section = section;
        this.startedAt = Instant.now();
    }

    public void end() {
        if (endedAt == null) {
            endedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public AcademicCohort getCohort() {
        return cohort;
    }

    public Semester getSemester() {
        return semester;
    }

    public Section getSection() {
        return section;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}
