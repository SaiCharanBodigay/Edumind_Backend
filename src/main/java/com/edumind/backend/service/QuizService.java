package com.edumind.backend.service;

import com.edumind.backend.dto.FacultyAttemptAnswerResponse;
import com.edumind.backend.dto.FacultyQuizAttemptResponse;
import com.edumind.backend.dto.QuizAnswerRequest;
import com.edumind.backend.dto.QuizAttemptAnswerResponse;
import com.edumind.backend.dto.QuizOptionRequest;
import com.edumind.backend.dto.QuizOptionResponse;
import com.edumind.backend.dto.QuizQuestionRequest;
import com.edumind.backend.dto.QuizQuestionResponse;
import com.edumind.backend.dto.QuizRequest;
import com.edumind.backend.dto.QuizResponse;
import com.edumind.backend.dto.ReleasedQuizAnswerResponse;
import com.edumind.backend.dto.ReleasedQuizResultResponse;
import com.edumind.backend.dto.StudentQuizAttemptResponse;
import com.edumind.backend.dto.StudentQuizAttemptSummary;
import com.edumind.backend.dto.StudentQuizOptionResponse;
import com.edumind.backend.dto.StudentQuizQuestionResponse;
import com.edumind.backend.dto.StudentQuizResponse;
import com.edumind.backend.entity.Course;
import com.edumind.backend.entity.Faculty;
import com.edumind.backend.entity.Quiz;
import com.edumind.backend.entity.QuizAnswer;
import com.edumind.backend.entity.QuizAttempt;
import com.edumind.backend.entity.QuizOption;
import com.edumind.backend.entity.QuizQuestion;
import com.edumind.backend.entity.Student;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.FacultyCourseAllocationRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.QuizAnswerRepository;
import com.edumind.backend.repository.QuizAttemptRepository;
import com.edumind.backend.repository.QuizRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {

    private static final BigDecimal MAX_MARKS = new BigDecimal("1000000.00");

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseAllocationRepository allocationRepository;
    private final StudentPortalService studentPortalService;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            QuizAnswerRepository answerRepository,
            FacultyRepository facultyRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            FacultyCourseAllocationRepository allocationRepository,
            StudentPortalService studentPortalService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.studentPortalService = studentPortalService;
    }

    @Transactional
    public List<QuizResponse> listForFaculty(String username, Long courseId) {
        Faculty faculty = requireFaculty(username);
        requireAllocatedCourse(faculty, courseId);
        List<Quiz> quizzes = quizRepository.findAllByCourseIdAndFacultyIdOrderByCreatedAtDesc(
                courseId, faculty.getId());
        quizzes.forEach(quiz -> attemptRepository
                .findAllByQuizIdAndStatus(quiz.getId(), "IN_PROGRESS")
                .forEach(this::expireIfNeeded));
        return quizzes.stream().map(this::facultyResponse).toList();
    }

    @Transactional
    public QuizResponse create(String username, Long courseId, QuizRequest request) {
        Faculty faculty = requireFaculty(username);
        Course course = requireAllocatedCourse(faculty, courseId);
        validateRequest(request);
        Quiz quiz = new Quiz(course, faculty, clean(request.title()), cleanNullable(request.description()),
                cleanNullable(request.instructions()), request.startsAt(), request.endsAt(),
                request.durationMinutes(), request.maxAttempts());
        quiz.replaceQuestions(buildQuestions(request.questions()));
        return facultyResponse(quizRepository.save(quiz));
    }

    @Transactional(readOnly = true)
    public QuizResponse getForFaculty(String username, Long quizId) {
        return facultyResponse(requireFacultyQuiz(username, quizId));
    }

    @Transactional
    public QuizResponse update(String username, Long quizId, QuizRequest request) {
        Quiz quiz = requireFacultyQuizForUpdate(username, quizId);
        if (attemptRepository.existsByQuizId(quizId)) {
            throw conflict("A quiz cannot be edited after a student has started an attempt.");
        }
        validateRequest(request);
        quiz.update(clean(request.title()), cleanNullable(request.description()),
                cleanNullable(request.instructions()), request.startsAt(), request.endsAt(),
                request.durationMinutes(), request.maxAttempts());
        quiz.replaceQuestions(buildQuestions(request.questions()));
        return facultyResponse(quizRepository.save(quiz));
    }

    @Transactional
    public QuizResponse publish(String username, Long quizId) {
        Quiz quiz = requireFacultyQuizForUpdate(username, quizId);
        validateQuizContent(quiz);
        quiz.publish();
        return facultyResponse(quiz);
    }

    @Transactional
    public QuizResponse unpublish(String username, Long quizId) {
        Quiz quiz = requireFacultyQuizForUpdate(username, quizId);
        List<QuizAttempt> activeAttempts = attemptRepository
                .findAllByQuizIdAndStatus(quizId, "IN_PROGRESS");
        activeAttempts.forEach(this::expireIfNeeded);
        if (activeAttempts.stream().anyMatch(attempt -> "IN_PROGRESS".equals(attempt.getStatus()))) {
            throw conflict("Active student attempts must finish or time out before unpublishing.");
        }
        quiz.unpublish();
        return facultyResponse(quiz);
    }

    @Transactional
    public QuizResponse releaseResults(String username, Long quizId) {
        Quiz quiz = requireFacultyQuizForUpdate(username, quizId);
        boolean availabilityEnded = quiz.getEndsAt() != null
                && !Instant.now().isBefore(quiz.getEndsAt());
        if (!availabilityEnded && !"DRAFT".equals(quiz.getStatus())) {
            throw conflict(
                    "Unpublish the quiz or wait until its availability window ends before releasing results.");
        }
        List<QuizAttempt> attempts = attemptRepository.findAllForQuiz(quizId);
        attempts.forEach(this::expireIfNeeded);
        if (attempts.stream().anyMatch(attempt -> "IN_PROGRESS".equals(attempt.getStatus()))) {
            throw conflict("Finish or time out all active attempts before releasing results.");
        }
        quiz.releaseResults();
        return facultyResponse(quiz);
    }

    @Transactional
    public void delete(String username, Long quizId) {
        Quiz quiz = requireFacultyQuizForUpdate(username, quizId);
        if (attemptRepository.existsByQuizId(quizId)) {
            throw conflict("A quiz with student attempts cannot be deleted.");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    public List<FacultyQuizAttemptResponse> listFacultyAttempts(String username, Long quizId) {
        Quiz quiz = requireFacultyQuiz(username, quizId);
        List<QuizAttempt> attempts = attemptRepository.findAllForQuiz(quiz.getId());
        attempts.forEach(this::expireIfNeeded);
        return attempts.stream().map(this::facultyAttemptResponse).toList();
    }

    @Transactional
    public List<StudentQuizResponse> listForStudent(String username) {
        Student student = requireStudent(username);
        List<Long> courseIds = studentPortalService.getCourses(username)
                .stream().map(course -> course.getId()).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return quizRepository.findPublishedForCourses(courseIds).stream()
                .map(quiz -> studentQuizResponse(quiz, student, false))
                .toList();
    }

    @Transactional
    public StudentQuizResponse getForStudent(String username, Long quizId) {
        Student student = requireStudent(username);
        Quiz quiz = requireQuiz(quizId);
        studentPortalService.requireStudentCourse(username, quiz.getCourse().getId());
        boolean hasPriorAttempts = attemptRepository.countByQuizIdAndStudentId(
                quizId, student.getId()) > 0;
        if (!"PUBLISHED".equals(quiz.getStatus()) && !hasPriorAttempts) {
            throw notFound("Quiz not found.");
        }
        return studentQuizResponse(quiz, student, true);
    }

    @Transactional
    public StudentQuizAttemptResponse startAttempt(String username, Long quizId) {
        Student student = requireStudent(username);
        studentPortalService.requireStudentCourse(username, requireQuiz(quizId).getCourse().getId());
        Quiz quiz = quizRepository.findByIdForUpdate(quizId)
                .orElseThrow(() -> notFound("Quiz not found."));

        var current = attemptRepository
                .findFirstByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                        quizId, student.getId(), "IN_PROGRESS");
        if (current.isPresent()) {
            QuizAttempt active = attemptRepository.findByIdForUpdate(current.get().getId())
                    .orElseThrow(() -> notFound("Quiz attempt not found."));
            if (!expireIfNeeded(active)) {
                return studentAttemptResponse(active, student);
            }
        }

        if (!"PUBLISHED".equals(quiz.getStatus())) {
            throw conflict("This quiz is not published.");
        }
        validateAvailabilityForStart(quiz);
        int attemptsUsed = attemptRepository.countByQuizIdAndStudentId(quizId, student.getId());
        if (attemptsUsed >= quiz.getMaxAttempts()) {
            throw conflict("You have used all allowed attempts for this quiz.");
        }

        QuizAttempt attempt = new QuizAttempt(
                quiz, student, attemptsUsed + 1, totalMarks(quiz));
        return studentAttemptResponse(attemptRepository.save(attempt), student);
    }

    @Transactional
    public StudentQuizAttemptResponse getStudentAttempt(String username, Long attemptId) {
        Student student = requireStudent(username);
        QuizAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> notFound("Quiz attempt not found."));
        studentPortalService.requireStudentCourse(username, attempt.getQuiz().getCourse().getId());
        expireIfNeeded(attempt);
        return studentAttemptResponse(attempt, student);
    }

    @Transactional
    public StudentQuizAttemptResponse saveAnswer(
            String username, Long attemptId, Long questionId, QuizAnswerRequest request) {
        Student student = requireStudent(username);
        QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> notFound("Quiz attempt not found."));
        requireAttemptOwner(attempt, username, student);
        if (expireIfNeeded(attempt)) {
            return studentAttemptResponse(attempt, student);
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw conflict("Submitted quiz attempts cannot be changed.");
        }
        QuizQuestion question = attempt.getQuiz().getQuestions().stream()
                .filter(item -> item.getId().equals(questionId))
                .findFirst().orElseThrow(() -> notFound("Question not found for this quiz."));
        QuizOption option = question.getOptions().stream()
                .filter(item -> item.getId().equals(request.optionId()))
                .findFirst().orElseThrow(() -> badRequest(
                        "Selected option does not belong to this question."));
        QuizAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> new QuizAnswer(attempt, question, option));
        answer.select(option);
        answerRepository.save(answer);
        return studentAttemptResponse(attempt, student);
    }

    @Transactional
    public StudentQuizAttemptResponse submit(String username, Long attemptId) {
        Student student = requireStudent(username);
        QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> notFound("Quiz attempt not found."));
        requireAttemptOwner(attempt, username, student);
        if (expireIfNeeded(attempt)) {
            return studentAttemptResponse(attempt, student);
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw conflict("This quiz attempt has already been submitted.");
        }
        attempt.finish("SUBMITTED", calculateScore(attempt));
        return studentAttemptResponse(attempt, student);
    }

    @Transactional(readOnly = true)
    public ReleasedQuizResultResponse getReleasedResult(String username, Long quizId, Long attemptId) {
        Student student = requireStudent(username);
        Quiz quiz = requireQuiz(quizId);
        studentPortalService.requireStudentCourse(username, quiz.getCourse().getId());
        if (!quiz.isResultsReleased()) {
            throw conflict("Results have not been released.");
        }
        QuizAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, student.getId())
                .filter(item -> item.getQuiz().getId().equals(quizId))
                .orElseThrow(() -> notFound("Quiz attempt not found."));
        if (attempt.getScore() == null) {
            throw conflict("This attempt has not been submitted.");
        }
        List<ReleasedQuizAnswerResponse> answers = quiz.getQuestions().stream()
                .map(question -> {
                    QuizAnswer answer = answerRepository
                            .findByAttemptIdAndQuestionId(attemptId, question.getId()).orElse(null);
                    QuizOption correct = question.getOptions().stream()
                            .filter(QuizOption::isCorrect).findFirst().orElseThrow();
                    return new ReleasedQuizAnswerResponse(
                            question.getId(), question.getText(), question.getMarks(),
                            answer == null ? null : answer.getSelectedOption().getId(),
                            answer == null ? null : answer.getSelectedOption().getText(),
                            correct.getId(), correct.getText(),
                            answer != null && answer.isCorrect());
                }).toList();
        return new ReleasedQuizResultResponse(
                attempt.getId(), attempt.getStatus(), attempt.getScore(),
                attempt.getMaxScore(), answers);
    }

    private StudentQuizResponse studentQuizResponse(Quiz quiz, Student student, boolean details) {
        List<QuizAttempt> attempts = attemptRepository
                .findAllByQuizIdAndStudentIdOrderByAttemptNumberDesc(quiz.getId(), student.getId());
        attempts.stream().filter(a -> "IN_PROGRESS".equals(a.getStatus())).forEach(this::expireIfNeeded);
        attempts = attemptRepository
                .findAllByQuizIdAndStudentIdOrderByAttemptNumberDesc(quiz.getId(), student.getId());
        QuizAttempt active = attempts.stream()
                .filter(a -> "IN_PROGRESS".equals(a.getStatus())).findFirst().orElse(null);
        String availability = availability(quiz, active, attempts.size());
        boolean canViewQuestions = details
                && ("AVAILABLE".equals(availability) || active != null);
        return new StudentQuizResponse(
                quiz.getId(), quiz.getCourse().getId(), quiz.getCourse().getCode(),
                quiz.getCourse().getName(), quiz.getTitle(), quiz.getDescription(),
                quiz.getInstructions(), quiz.getStartsAt(), quiz.getEndsAt(),
                quiz.getDurationMinutes(), quiz.getMaxAttempts(), totalMarks(quiz),
                availability, attempts.size(), active == null ? null : active.getId(),
                quiz.isResultsReleased(),
                attempts.stream().map(a -> new StudentQuizAttemptSummary(
                        a.getId(), a.getAttemptNumber(), a.getStatus(), a.getStartedAt(),
                        a.getSubmittedAt(),
                        quiz.isResultsReleased() ? a.getScore() : null,
                        quiz.isResultsReleased() ? a.getMaxScore() : null))
                        .toList(),
                canViewQuestions ? quiz.getQuestions().stream()
                        .map(this::studentQuestionResponse).toList() : List.of());
    }

    private String availability(Quiz quiz, QuizAttempt active, int attemptsUsed) {
        if (active != null) return "IN_PROGRESS";
        if (!"PUBLISHED".equals(quiz.getStatus())) return "UNPUBLISHED";
        Instant now = Instant.now();
        if (quiz.getStartsAt() != null && now.isBefore(quiz.getStartsAt())) return "UPCOMING";
        if (quiz.getEndsAt() != null && !now.isBefore(quiz.getEndsAt())) return "CLOSED";
        if (attemptsUsed >= quiz.getMaxAttempts()) return "ATTEMPTS_EXHAUSTED";
        return "AVAILABLE";
    }

    private void validateAvailabilityForStart(Quiz quiz) {
        Instant now = Instant.now();
        if (quiz.getStartsAt() != null && now.isBefore(quiz.getStartsAt())) {
            throw conflict("This quiz is not available yet.");
        }
        if (quiz.getEndsAt() != null && !now.isBefore(quiz.getEndsAt())) {
            throw conflict("The quiz availability window has ended.");
        }
    }

    private boolean expireIfNeeded(QuizAttempt attempt) {
        if ("IN_PROGRESS".equals(attempt.getStatus())
                && attempt.getDeadline() != null
                && !Instant.now().isBefore(attempt.getDeadline())) {
            attempt.finish("TIMED_OUT", calculateScore(attempt));
            return true;
        }
        return false;
    }

    private BigDecimal calculateScore(QuizAttempt attempt) {
        BigDecimal score = BigDecimal.ZERO;
        for (QuizAnswer answer : answerRepository.findAllByAttemptIdOrderByQuestionPositionAsc(
                attempt.getId())) {
            if (answer.isCorrect()) {
                score = score.add(answer.getQuestion().getMarks());
            }
        }
        return score;
    }

    private void validateRequest(QuizRequest request) {
        if (request.maxAttempts() == null || request.maxAttempts() < 1 || request.maxAttempts() > 20) {
            throw badRequest("Maximum attempts must be between 1 and 20.");
        }
        if (request.durationMinutes() != null
                && (request.durationMinutes() < 1 || request.durationMinutes() > 600)) {
            throw badRequest("Duration must be between 1 and 600 minutes, or empty for untimed.");
        }
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw badRequest("Quiz end time must be later than its start time.");
        }
        if (request.questions() == null || request.questions().isEmpty()
                || request.questions().size() > 100) {
            throw badRequest("A quiz must contain between 1 and 100 questions.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (QuizQuestionRequest question : request.questions()) {
            validateQuestion(question);
            total = total.add(question.marks());
        }
        validateTotalMarks(total);
    }

    private void validateQuestion(QuizQuestionRequest question) {
        if (question.marks() == null || question.marks().compareTo(BigDecimal.ZERO) <= 0
                || question.marks().compareTo(MAX_MARKS) > 0
                || question.marks().stripTrailingZeros().scale() > 2) {
            throw badRequest("Each question must have positive marks with at most two decimals.");
        }
        if (question.options() == null || question.options().size() < 2
                || question.options().size() > 6) {
            throw badRequest("Each question must have between 2 and 6 options.");
        }
        long correctCount = question.options().stream().filter(QuizOptionRequest::correct).count();
        if (correctCount != 1) {
            throw badRequest("Each multiple-choice question must have exactly one correct option.");
        }
    }

    private void validateQuizContent(Quiz quiz) {
        if (quiz.getQuestions().isEmpty()) {
            throw badRequest("Add at least one question before publishing.");
        }
        BigDecimal total = totalMarks(quiz);
        validateTotalMarks(total);
        if (quiz.getStartsAt() != null && quiz.getEndsAt() != null
                && !quiz.getEndsAt().isAfter(quiz.getStartsAt())) {
            throw badRequest("Quiz end time must be later than its start time.");
        }
    }

    private void validateTotalMarks(BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0 || total.compareTo(MAX_MARKS) > 0) {
            throw badRequest("Total quiz marks must be positive and no more than 1,000,000.");
        }
    }

    private List<QuizQuestion> buildQuestions(List<QuizQuestionRequest> requests) {
        List<QuizQuestion> questions = new ArrayList<>();
        int questionPosition = 0;
        for (QuizQuestionRequest request : requests) {
            QuizQuestion question = new QuizQuestion(
                    clean(request.text()), request.marks(), questionPosition++);
            List<QuizOption> options = new ArrayList<>();
            int optionPosition = 0;
            for (QuizOptionRequest optionRequest : request.options()) {
                options.add(new QuizOption(
                        clean(optionRequest.text()), optionPosition++, optionRequest.correct()));
            }
            question.replaceOptions(options);
            questions.add(question);
        }
        return questions;
    }

    private QuizResponse facultyResponse(Quiz quiz) {
        long attemptCount = attemptRepository.countByQuizId(quiz.getId());
        long activeAttemptCount = attemptRepository.countByQuizIdAndStatus(
                quiz.getId(), "IN_PROGRESS");
        boolean availabilityEnded = quiz.getEndsAt() != null
                && !Instant.now().isBefore(quiz.getEndsAt());
        boolean canReleaseResults = attemptCount > 0 && activeAttemptCount == 0
                && (availabilityEnded || "DRAFT".equals(quiz.getStatus()));
        return new QuizResponse(
                quiz.getId(), quiz.getCourse().getId(), quiz.getCourse().getCode(),
                quiz.getCourse().getName(), quiz.getTitle(), quiz.getDescription(),
                quiz.getInstructions(), quiz.getStatus(), quiz.getStartsAt(), quiz.getEndsAt(),
                quiz.getDurationMinutes(), quiz.getMaxAttempts(), totalMarks(quiz),
                quiz.isResultsReleased(), quiz.getQuestions().size(), attemptCount,
                activeAttemptCount, canReleaseResults,
                quiz.getQuestions().stream().map(this::facultyQuestionResponse).toList());
    }

    private QuizQuestionResponse facultyQuestionResponse(QuizQuestion question) {
        return new QuizQuestionResponse(
                question.getId(), question.getText(), question.getMarks(), question.getPosition(),
                question.getOptions().stream().map(this::facultyOptionResponse).toList());
    }

    private QuizOptionResponse facultyOptionResponse(QuizOption option) {
        return new QuizOptionResponse(option.getId(), option.getText(), option.isCorrect());
    }

    private StudentQuizQuestionResponse studentQuestionResponse(QuizQuestion question) {
        return new StudentQuizQuestionResponse(
                question.getId(), question.getText(), question.getMarks(), question.getPosition(),
                question.getOptions().stream()
                        .map(option -> new StudentQuizOptionResponse(option.getId(), option.getText()))
                        .toList());
    }

    private StudentQuizAttemptResponse studentAttemptResponse(QuizAttempt attempt, Student student) {
        boolean released = attempt.getQuiz().isResultsReleased();
        List<QuizAttemptAnswerResponse> answers = attempt.getQuiz().getQuestions().stream()
                .map(question -> {
                    QuizAnswer answer = answerRepository.findByAttemptIdAndQuestionId(
                            attempt.getId(), question.getId()).orElse(null);
                    return new QuizAttemptAnswerResponse(
                            question.getId(), question.getText(), question.getMarks(),
                            answer == null ? null : answer.getSelectedOption().getId(),
                            question.getOptions().stream()
                                    .map(option -> new StudentQuizOptionResponse(
                                            option.getId(), option.getText()))
                                    .toList());
                }).toList();
        return new StudentQuizAttemptResponse(
                attempt.getId(), attempt.getQuiz().getId(), attempt.getQuiz().getTitle(),
                attempt.getAttemptNumber(), attempt.getStatus(), attempt.getStartedAt(),
                attempt.getDeadline(), attempt.getSubmittedAt(),
                released ? attempt.getScore() : null,
                released ? attempt.getMaxScore() : null, released, answers);
    }

    private FacultyQuizAttemptResponse facultyAttemptResponse(QuizAttempt attempt) {
        List<FacultyAttemptAnswerResponse> answers = attempt.getQuiz().getQuestions().stream()
                .map(question -> {
                    QuizAnswer answer = answerRepository.findByAttemptIdAndQuestionId(
                            attempt.getId(), question.getId()).orElse(null);
                    QuizOption correct = question.getOptions().stream()
                            .filter(QuizOption::isCorrect).findFirst().orElseThrow();
                    return new FacultyAttemptAnswerResponse(
                            question.getId(), question.getText(), question.getMarks(),
                            answer == null ? null : answer.getSelectedOption().getId(),
                            correct.getId(),
                            answer == null ? null : answer.isCorrect(),
                            question.getOptions().stream().map(this::facultyOptionResponse).toList());
                }).toList();
        return new FacultyQuizAttemptResponse(
                attempt.getId(), attempt.getStudent().getId(),
                attempt.getStudent().getUser().getFullName(),
                attempt.getStudent().getRollNumber(), attempt.getAttemptNumber(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.getDeadline(),
                attempt.getSubmittedAt(), attempt.getScore(), attempt.getMaxScore(), answers);
    }

    private BigDecimal totalMarks(Quiz quiz) {
        return quiz.getQuestions().stream()
                .map(QuizQuestion::getMarks)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Faculty requireFaculty(String username) {
        return facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> notFound("Faculty profile not found."));
    }

    private Student requireStudent(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> notFound("Student profile not found."));
    }

    private Course requireAllocatedCourse(Faculty faculty, Long courseId) {
        if (!allocationRepository.existsByFacultyIdAndCourseId(faculty.getId(), courseId)) {
            throw notFound("Assigned course not found.");
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> notFound("Assigned course not found."));
    }

    private Quiz requireFacultyQuiz(String username, Long quizId) {
        Faculty faculty = requireFaculty(username);
        Quiz quiz = quizRepository.findByIdAndFacultyId(quizId, faculty.getId())
                .orElseThrow(() -> notFound("Quiz not found."));
        requireAllocatedCourse(faculty, quiz.getCourse().getId());
        return quiz;
    }

    private Quiz requireFacultyQuizForUpdate(String username, Long quizId) {
        Faculty faculty = requireFaculty(username);
        Quiz quiz = quizRepository.findByIdForUpdate(quizId)
                .orElseThrow(() -> notFound("Quiz not found."));
        if (!quiz.getFaculty().getId().equals(faculty.getId())
                || !allocationRepository.existsByFacultyIdAndCourseId(
                        faculty.getId(), quiz.getCourse().getId())) {
            throw notFound("Quiz not found.");
        }
        return quiz;
    }

    private Quiz requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> notFound("Quiz not found."));
    }

    private void requireAttemptOwner(QuizAttempt attempt, String username, Student student) {
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw notFound("Quiz attempt not found.");
        }
        studentPortalService.requireStudentCourse(username, attempt.getQuiz().getCourse().getId());
    }

    private String clean(String value) {
        return value.trim();
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
