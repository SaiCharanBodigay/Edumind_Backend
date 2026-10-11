package com.edumind.backend;

import com.edumind.backend.dto.QuizAnswerRequest;
import com.edumind.backend.dto.QuizOptionRequest;
import com.edumind.backend.dto.QuizQuestionRequest;
import com.edumind.backend.dto.QuizRequest;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.Quiz;
import com.edumind.backend.entity.QuizAnswer;
import com.edumind.backend.entity.QuizAttempt;
import com.edumind.backend.entity.QuizOption;
import com.edumind.backend.entity.QuizQuestion;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.QuizAnswerRepository;
import com.edumind.backend.repository.QuizAttemptRepository;
import com.edumind.backend.repository.QuizRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.QuizService;
import com.edumind.backend.service.StudentPortalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizServiceTests {

    @Test
    void facultyCannotCreateQuizForUnallocatedCourse() {
        Fixture fixture = new Fixture();
        when(fixture.faculties.findByUserUsername("faculty"))
                .thenReturn(Optional.of(fixture.faculty));
        when(fixture.allocations.existsByFacultyIdAndCourseId(7L, 11L)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.create("faculty", 11L, validRequest()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(fixture.courses, never()).findById(any());
    }

    @Test
    void quizQuestionMustHaveExactlyOneCorrectOption() {
        Fixture fixture = new Fixture();
        when(fixture.faculties.findByUserUsername("faculty"))
                .thenReturn(Optional.of(fixture.faculty));
        when(fixture.allocations.existsByFacultyIdAndCourseId(7L, 11L)).thenReturn(true);
        when(fixture.courses.findById(11L)).thenReturn(Optional.of(
                new Course("Data Structures", "DS", 3, null)));
        QuizRequest request = new QuizRequest(
                "Quiz", null, null, null, null, null, 1,
                List.of(new QuizQuestionRequest("Question", BigDecimal.ONE, List.of(
                        new QuizOptionRequest("First", false),
                        new QuizOptionRequest("Second", false)))));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.create("faculty", 11L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(fixture.quizzes, never()).save(any());
    }

    @Test
    void scoreIsCalculatedFromSavedAnswersOnServer() {
        Fixture fixture = new Fixture();
        Student student = mock(Student.class);
        QuizAttempt attempt = mock(QuizAttempt.class);
        Quiz quiz = mock(Quiz.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        QuizQuestion correctQuestion = mock(QuizQuestion.class);
        QuizQuestion incorrectQuestion = mock(QuizQuestion.class);
        QuizAnswer correctAnswer = mock(QuizAnswer.class);
        QuizAnswer incorrectAnswer = mock(QuizAnswer.class);

        when(fixture.students.findByUserUsername("student")).thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(21L);
        when(fixture.attempts.findByIdForUpdate(31L)).thenReturn(Optional.of(attempt));
        when(attempt.getStudent()).thenReturn(student);
        when(attempt.getId()).thenReturn(31L);
        when(attempt.getQuiz()).thenReturn(quiz);
        when(attempt.getStatus()).thenReturn("IN_PROGRESS");
        when(attempt.getDeadline()).thenReturn(null);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.isResultsReleased()).thenReturn(false);
        when(quiz.getQuestions()).thenReturn(List.of());
        when(correctAnswer.isCorrect()).thenReturn(true);
        when(correctAnswer.getQuestion()).thenReturn(correctQuestion);
        when(correctQuestion.getMarks()).thenReturn(new BigDecimal("2.50"));
        when(incorrectAnswer.isCorrect()).thenReturn(false);
        when(fixture.answers.findAllByAttemptIdOrderByQuestionPositionAsc(31L))
                .thenReturn(List.of(correctAnswer, incorrectAnswer));

        fixture.service.submit("student", 31L);

        verify(fixture.studentPortal).requireStudentCourse("student", 11L);
        verify(attempt).finish(eq("SUBMITTED"), eq(new BigDecimal("2.50")));
    }

    @Test
    void selectedOptionMustBelongToTheAttemptQuestion() {
        Fixture fixture = new Fixture();
        Student student = mock(Student.class);
        QuizAttempt attempt = mock(QuizAttempt.class);
        Quiz quiz = mock(Quiz.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        QuizQuestion question = mock(QuizQuestion.class);
        QuizOption option = mock(QuizOption.class);
        when(fixture.students.findByUserUsername("student")).thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(21L);
        when(fixture.attempts.findByIdForUpdate(31L)).thenReturn(Optional.of(attempt));
        when(attempt.getStudent()).thenReturn(student);
        when(attempt.getQuiz()).thenReturn(quiz);
        when(attempt.getStatus()).thenReturn("IN_PROGRESS");
        when(attempt.getDeadline()).thenReturn(null);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getQuestions()).thenReturn(List.of(question));
        when(question.getId()).thenReturn(41L);
        when(question.getOptions()).thenReturn(List.of(option));
        when(option.getId()).thenReturn(99L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.saveAnswer(
                        "student", 31L, 41L, new QuizAnswerRequest(100L)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(fixture.answers, never()).save(any());
    }

    @Test
    void maximumAttemptCountIsEnforcedOnTheServer() {
        Fixture fixture = new Fixture();
        Student student = mock(Student.class);
        Quiz quiz = mock(Quiz.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        when(fixture.students.findByUserUsername("student")).thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(21L);
        when(fixture.quizzes.findById(55L)).thenReturn(Optional.of(quiz));
        when(fixture.quizzes.findByIdForUpdate(55L)).thenReturn(Optional.of(quiz));
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getStatus()).thenReturn("PUBLISHED");
        when(quiz.getMaxAttempts()).thenReturn(1);
        when(fixture.attempts
                .findFirstByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                        55L, 21L, "IN_PROGRESS"))
                .thenReturn(Optional.empty());
        when(fixture.attempts.countByQuizIdAndStudentId(55L, 21L)).thenReturn(1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.startAttempt("student", 55L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(fixture.attempts, never()).save(any());
    }

    @Test
    void expiredAttemptIsFinalizedAsTimedOutByTheServer() {
        Fixture fixture = new Fixture();
        Student student = mock(Student.class);
        QuizAttempt attempt = mock(QuizAttempt.class);
        Quiz quiz = mock(Quiz.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        when(fixture.students.findByUserUsername("student")).thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(21L);
        when(fixture.attempts.findByIdAndStudentId(31L, 21L)).thenReturn(Optional.of(attempt));
        when(attempt.getStudent()).thenReturn(student);
        when(attempt.getQuiz()).thenReturn(quiz);
        when(attempt.getStatus()).thenReturn("IN_PROGRESS");
        when(attempt.getAttemptNumber()).thenReturn(1);
        when(attempt.getDeadline()).thenReturn(Instant.now().minusSeconds(1));
        when(attempt.getId()).thenReturn(31L);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getQuestions()).thenReturn(List.of());
        when(quiz.isResultsReleased()).thenReturn(false);
        when(fixture.answers.findAllByAttemptIdOrderByQuestionPositionAsc(31L))
                .thenReturn(List.of());

        fixture.service.getStudentAttempt("student", 31L);

        verify(attempt).finish(eq("TIMED_OUT"), eq(BigDecimal.ZERO));
    }

    @Test
    void resultsCannotBeReleasedWhileQuizIsAvailableAndAnAttemptIsActive() {
        Fixture fixture = new Fixture();
        Quiz quiz = mock(Quiz.class);
        QuizAttempt activeAttempt = mock(QuizAttempt.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        when(fixture.faculties.findByUserUsername("faculty"))
                .thenReturn(Optional.of(fixture.faculty));
        when(fixture.quizzes.findByIdForUpdate(55L)).thenReturn(Optional.of(quiz));
        when(quiz.getFaculty()).thenReturn(fixture.faculty);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getStatus()).thenReturn("PUBLISHED");
        when(quiz.getEndsAt()).thenReturn(null);
        when(fixture.allocations.existsByFacultyIdAndCourseId(7L, 11L)).thenReturn(true);
        when(fixture.attempts.findAllForQuiz(55L)).thenReturn(List.of(activeAttempt));
        when(activeAttempt.getStatus()).thenReturn("IN_PROGRESS");
        when(activeAttempt.getDeadline()).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.releaseResults("faculty", 55L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(quiz, never()).releaseResults();
    }

    @Test
    void quizCannotBeUnpublishedWhileAStudentAttemptIsActive() {
        Fixture fixture = new Fixture();
        Quiz quiz = mock(Quiz.class);
        QuizAttempt activeAttempt = mock(QuizAttempt.class);
        Course course = new Course("Data Structures", "DS", 3, null);
        course.setId(11L);
        when(fixture.faculties.findByUserUsername("faculty"))
                .thenReturn(Optional.of(fixture.faculty));
        when(fixture.quizzes.findByIdForUpdate(55L)).thenReturn(Optional.of(quiz));
        when(quiz.getFaculty()).thenReturn(fixture.faculty);
        when(quiz.getCourse()).thenReturn(course);
        when(fixture.allocations.existsByFacultyIdAndCourseId(7L, 11L)).thenReturn(true);
        when(fixture.attempts.findAllByQuizIdAndStatus(55L, "IN_PROGRESS"))
                .thenReturn(List.of(activeAttempt));
        when(activeAttempt.getStatus()).thenReturn("IN_PROGRESS");
        when(activeAttempt.getDeadline()).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> fixture.service.unpublish("faculty", 55L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(quiz, never()).unpublish();
    }

    private QuizRequest validRequest() {
        return new QuizRequest("Quiz", null, null, null, null, null, 1,
                List.of(new QuizQuestionRequest("Question", BigDecimal.ONE, List.of(
                        new QuizOptionRequest("First", true),
                        new QuizOptionRequest("Second", false)))));
    }

    private static class Fixture {
        private final QuizRepository quizzes = mock(QuizRepository.class);
        private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
        private final QuizAnswerRepository answers = mock(QuizAnswerRepository.class);
        private final FacultyRepository faculties = mock(FacultyRepository.class);
        private final StudentRepository students = mock(StudentRepository.class);
        private final CourseRepository courses = mock(CourseRepository.class);
        private final FacultyCourseAllocationRepository allocations =
                mock(FacultyCourseAllocationRepository.class);
        private final StudentPortalService studentPortal = mock(StudentPortalService.class);
        private final Faculty faculty = new Faculty(
                "EMP-7", "Instructor", new User("faculty", "hash", "FACULTY", "Faculty", null),
                new Department("Engineering", "ENG"));
        private final QuizService service;

        private Fixture() {
            faculty.setId(7L);
            service = new QuizService(
                    quizzes, attempts, answers, faculties, students, courses,
                    allocations, studentPortal);
        }
    }
}
