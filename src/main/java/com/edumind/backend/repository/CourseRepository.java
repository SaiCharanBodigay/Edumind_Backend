package com.edumind.backend.repository;

import com.edumind.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository
        extends JpaRepository<Course, Long> {

    boolean existsByCode(String code);

    boolean existsBySemesterId(Long semesterId);
}
