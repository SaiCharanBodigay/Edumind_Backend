package com.edumind.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "faculty_course_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"faculty_id", "course_id"}
        )
    }
)
public class FacultyCourseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public FacultyCourseAllocation() {
    }

    public FacultyCourseAllocation(Faculty faculty, Course course) {
        this.faculty = faculty;
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
