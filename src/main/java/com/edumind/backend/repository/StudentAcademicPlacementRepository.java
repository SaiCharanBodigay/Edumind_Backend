package com.edumind.backend.repository;

import com.edumind.backend.entity.StudentAcademicPlacement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentAcademicPlacementRepository
        extends JpaRepository<StudentAcademicPlacement, Long> {

    Optional<StudentAcademicPlacement> findFirstByStudentIdAndEndedAtIsNullOrderByStartedAtDesc(
            Long studentId);

    boolean existsBySectionId(Long sectionId);

    boolean existsByCohortId(Long cohortId);
}
