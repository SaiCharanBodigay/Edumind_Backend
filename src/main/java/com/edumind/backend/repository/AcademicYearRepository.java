package com.edumind.backend.repository;

import com.edumind.backend.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository
        extends JpaRepository<AcademicYear, Long> {

    boolean existsByName(String name);
}
