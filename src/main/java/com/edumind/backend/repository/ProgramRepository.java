package com.edumind.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumind.backend.entity.Program;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    boolean existsByCode(String code);

    boolean existsByDepartmentId(Long departmentId);
}
