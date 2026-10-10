package com.edumind.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumind.backend.entity.Faculty;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    boolean existsByEmployeeId(String employeeId);

    boolean existsByDepartmentId(Long departmentId);

    Optional<Faculty> findByUserUsername(String username);
}
