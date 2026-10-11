package com.edumind.backend.repository;

import com.edumind.backend.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "user", "department", "program", "semester",
            "semester.academicYear", "cohort", "section"
    })
    List<Student> findAll();

    @Override
    @EntityGraph(attributePaths = {
            "user", "department", "program", "semester",
            "semester.academicYear", "cohort", "section"
    })
    Optional<Student> findById(Long id);

    boolean existsByRollNumber(String rollNumber);

    boolean existsBySemesterId(Long semesterId);

    boolean existsByCohortId(Long cohortId);

    boolean existsBySectionId(Long sectionId);

    Optional<Student> findByUserUsername(String username);

    List<Student> findAllBySectionIdOrderByRollNumberAsc(Long sectionId);

    @Query("""
            select s from Student s
            where (:term is null
                or lower(s.rollNumber) like concat('%', lower(:term), '%')
                or lower(s.user.fullName) like concat('%', lower(:term), '%')
                or lower(s.user.username) like concat('%', lower(:term), '%')
                or lower(s.user.email) like concat('%', lower(:term), '%'))
              and (:departmentId is null or s.department.id = :departmentId)
              and (:programId is null or s.program.id = :programId)
              and (:cohortId is null or s.cohort.id = :cohortId)
              and (:semesterId is null or s.semester.id = :semesterId)
              and (:sectionId is null or s.section.id = :sectionId)
            """)
    @EntityGraph(attributePaths = {
            "user", "department", "program", "semester",
            "semester.academicYear", "cohort", "section"
    })
    Page<Student> search(
            @Param("term") String term,
            @Param("departmentId") Long departmentId,
            @Param("programId") Long programId,
            @Param("cohortId") Long cohortId,
            @Param("semesterId") Long semesterId,
            @Param("sectionId") Long sectionId,
            Pageable pageable);
}
