package com.edumind.backend.repository;

import com.edumind.backend.entity.AcademicCohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicCohortRepository extends JpaRepository<AcademicCohort, Long> {

    boolean existsByProgramIdAndNameIgnoreCase(Long programId, String name);

    List<AcademicCohort> findAllByProgramIdOrderByName(Long programId);

    boolean existsByProgramId(Long programId);
}
