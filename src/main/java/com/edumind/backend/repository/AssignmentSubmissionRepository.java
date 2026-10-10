package com.edumind.backend.repository;

import com.edumind.backend.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsByAssignmentId(Long assignmentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    long countByAssignmentId(Long assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(
            Long assignmentId, Long studentId);

    Optional<AssignmentSubmission> findByIdAndStudentId(Long id, Long studentId);

    List<AssignmentSubmission> findAllByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);

    Optional<AssignmentSubmission> findByIdAndAssignmentId(Long id, Long assignmentId);
}
