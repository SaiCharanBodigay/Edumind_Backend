package com.edumind.backend.repository;

import com.edumind.backend.entity.ClassInchargeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassInchargeAssignmentRepository
        extends JpaRepository<ClassInchargeAssignment, Long> {

    Optional<ClassInchargeAssignment> findFirstBySectionIdAndActiveTrue(Long sectionId);

    @Query("""
            select a from ClassInchargeAssignment a
            where a.active = true
            order by a.section.cohort.program.name, a.section.cohort.name, a.section.name
            """)
    List<ClassInchargeAssignment> findAllActive();

    @Query("""
            select a from ClassInchargeAssignment a
            where a.faculty.user.username = :username and a.active = true
            order by a.section.cohort.name, a.section.name
            """)
    List<ClassInchargeAssignment> findAllActiveByFacultyUsername(
            @Param("username") String username);

    boolean existsByFacultyUserUsernameAndSectionIdAndActiveTrue(
            String username, Long sectionId);

    boolean existsByFacultyIdAndActiveTrue(Long facultyId);

    boolean existsByFacultyId(Long facultyId);

    boolean existsBySectionId(Long sectionId);

    List<ClassInchargeAssignment> findAllByFacultyIdAndActiveTrue(Long facultyId);
}
