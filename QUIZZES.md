# Quizzes and online assessments

Quiz tables are additive and follow the existing Hibernate schema-update convention. Deploy against a backed-up staging database first; this feature does not drop or rewrite existing academic, assignment, or submission data.

## Authorization and API

- Faculty endpoints are under `/api/faculty`; each quiz is tied to its creating faculty member and an allocated course.
- Student endpoints are under `/api/student`; course membership is checked against the authenticated student's current program and semester. Student IDs are never accepted from requests.
- Quiz and attempt endpoints use the existing JWT role rules in `SecurityConfig`.
- Student quiz/question/attempt DTOs contain option IDs and text only. Correct-option IDs, correctness flags, and scores are available only through faculty review or the student's own result endpoint after faculty release.

## Attempt policy

- Quiz content can be edited only until the first attempt starts. A quiz with attempts cannot be deleted.
- The quiz row is locked while an attempt is created, enforcing attempt limits and numbering when requests race. The attempt row is locked while answers are saved or an attempt is submitted.
- Answers are saved per question and can be resumed. The server validates that each selected option belongs to the question in that quiz.
- Duration and quiz availability end time are enforced server-side. The earlier deadline applies. At timeout, saved answers are automatically scored and the attempt is marked `TIMED_OUT`; unsaved answers are not accepted.
- Scores are derived from persisted selected options and configured question marks, never from client-submitted scores.
- Results are released once for the whole quiz. Release is blocked while the quiz is still available unless it is a draft, and all active attempts must be finished or timed out. Unpublishing is blocked while active attempts remain, so students can continue to find and resume them. Release is irreversible because students may have already viewed the answer key.

## Main routes

Faculty: `/faculty/quizzes` supports course-scoped authoring, publication, attempt review, and result release.

Student: `/student/quizzes` supports browsing and starting quizzes; in-progress attempts are resumable at `/student/quizzes/:quizId/attempt/:attemptId`, with released results at `/student/quizzes/:quizId/results/:attemptId`.
