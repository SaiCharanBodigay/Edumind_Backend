package com.edumind.backend.repository;

import com.edumind.backend.entity.Quiz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByCourseIdAndFacultyIdOrderByCreatedAtDesc(Long courseId, Long facultyId);

    @Query("""
            select q from Quiz q
            where q.course.id in :courseIds and q.status = 'PUBLISHED'
            order by q.startsAt asc, q.createdAt desc
            """)
    List<Quiz> findPublishedForCourses(@Param("courseIds") Collection<Long> courseIds);

    Optional<Quiz> findByIdAndFacultyId(Long id, Long facultyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findByIdForUpdate(@Param("id") Long id);
}
