package com.edumind.backend.repository;

import com.edumind.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRollNumber(String rollNumber);

    boolean existsBySemesterId(Long semesterId);
}
