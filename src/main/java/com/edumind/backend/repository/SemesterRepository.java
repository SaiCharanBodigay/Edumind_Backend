package com.edumind.backend.repository;

import com.edumind.backend.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    boolean existsBySemesterNumberAndProgramIdAndAcademicYearId(
            Integer semesterNumber,
            Long programId,
            Long academicYearId
    );

    boolean existsByAcademicYearId(Long academicYearId);
}
