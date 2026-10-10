package com.edumind.backend.repository;

import com.edumind.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);

    boolean existsByName(String name);
}