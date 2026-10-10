package com.edumind.backend.repository;

import com.edumind.backend.entity.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseAssignmentRepository
        extends JpaRepository<CourseAssignment, Long> {

    List<CourseAssignment> findAllByCourseIdAndFacultyIdOrderByDueAtAsc(
            Long courseId, Long facultyId);

    List<CourseAssignment> findAllByCourseIdInOrderByDueAtAsc(
            Collection<Long> courseIds);

    Optional<CourseAssignment> findByIdAndFacultyId(Long id, Long facultyId);
}
