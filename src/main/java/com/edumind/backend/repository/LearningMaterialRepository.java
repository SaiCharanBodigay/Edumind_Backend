package com.edumind.backend.repository;

import com.edumind.backend.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningMaterialRepository
        extends JpaRepository<LearningMaterial, Long> {

    List<LearningMaterial> findAllByCourseIdOrderByUploadedAtDesc(Long courseId);

    Optional<LearningMaterial> findByIdAndFacultyId(Long id, Long facultyId);

    long countByCourseId(Long courseId);
}
