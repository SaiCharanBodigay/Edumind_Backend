package com.edumind.backend.repository;

import com.edumind.backend.entity.QuizAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    long countByQuizId(Long quizId);

    long countByQuizIdAndStatus(Long quizId, String status);

    int countByQuizIdAndStudentId(Long quizId, Long studentId);

    boolean existsByQuizId(Long quizId);

    @Query("""
            select a from QuizAttempt a
            join fetch a.student s
            join fetch s.user
            where a.quiz.id = :quizId
            order by s.rollNumber, a.attemptNumber
            """)
    List<QuizAttempt> findAllForQuiz(@Param("quizId") Long quizId);

    List<QuizAttempt> findAllByQuizIdAndStudentIdOrderByAttemptNumberDesc(Long quizId, Long studentId);

    List<QuizAttempt> findAllByQuizIdAndStatus(Long quizId, String status);

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
            Long quizId, Long studentId, String status);

    Optional<QuizAttempt> findByIdAndStudentId(Long id, Long studentId);

    Optional<QuizAttempt> findByIdAndQuizId(Long id, Long quizId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QuizAttempt a where a.id = :id")
    Optional<QuizAttempt> findByIdForUpdate(@Param("id") Long id);
}
