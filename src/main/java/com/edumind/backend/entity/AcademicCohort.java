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
@Table(
        name = "academic_cohorts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_cohort_program_name",
                columnNames = {"program_id", "name"}))
public class AcademicCohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    protected AcademicCohort() {
    }

    public AcademicCohort(String name, Program program) {
        this.name = name;
        this.program = program;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Program getProgram() {
        return program;
    }
}
