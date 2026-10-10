package com.edumind.backend.repository;

import com.edumind.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRollNumber(String rollNumber);

    boolean existsBySemesterId(Long semesterId);

    Optional<Student> findByUserUsername(String username);
}
