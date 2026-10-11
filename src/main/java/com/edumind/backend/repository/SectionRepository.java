package com.edumind.backend.repository;

import com.edumind.backend.entity.Section;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    boolean existsByCohortIdAndSemesterIdAndNameIgnoreCase(
            Long cohortId, Long semesterId, String name);

    boolean existsByCohortId(Long cohortId);

    List<Section> findAllByCohortProgramIdAndCohortIdAndSemesterIdOrderByName(
            Long programId, Long cohortId, Long semesterId);

    @Query("""
            select s from Section s
            where (:programId is null or s.cohort.program.id = :programId)
              and (:cohortId is null or s.cohort.id = :cohortId)
              and (:semesterId is null or s.semester.id = :semesterId)
            order by s.cohort.program.name, s.cohort.name, s.semester.semesterNumber, s.name
            """)
    List<Section> findFiltered(
            @Param("programId") Long programId,
            @Param("cohortId") Long cohortId,
            @Param("semesterId") Long semesterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Section s where s.id = :id")
    Optional<Section> findByIdForUpdate(@Param("id") Long id);
}
