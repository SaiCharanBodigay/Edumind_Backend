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

import java.time.LocalDate;

@Entity
@Table(
        name = "class_incharge_assignments",
        indexes = {
                @jakarta.persistence.Index(
                        name = "idx_class_incharge_section_active",
                        columnList = "section_id,active"),
                @jakarta.persistence.Index(
                        name = "idx_class_incharge_faculty_active",
                        columnList = "faculty_id,active")
        })
public class ClassInchargeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    protected ClassInchargeAssignment() {
    }

    public ClassInchargeAssignment(
            Faculty faculty,
            Section section,
            LocalDate startDate) {
        this.faculty = faculty;
        this.section = section;
        this.startDate = startDate;
    }

    public void end(LocalDate endDate) {
        active = false;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public Section getSection() {
        return section;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }
}
