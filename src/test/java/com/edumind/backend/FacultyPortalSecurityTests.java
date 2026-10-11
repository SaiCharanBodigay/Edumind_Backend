package com.edumind.backend;

import com.edumind.backend.dto.FacultyPortalCourseResponse;
import com.edumind.backend.dto.FacultyPortalProfileResponse;
import com.edumind.backend.dto.LearningMaterialResponse;
import com.edumind.backend.dto.StudentPortalCourseResponse;
import com.edumind.backend.dto.StudentQuizOptionResponse;
import com.edumind.backend.dto.StudentQuizQuestionResponse;
import com.edumind.backend.dto.StudentQuizResponse;
import com.edumind.backend.service.FacultyPortalService;
import com.edumind.backend.service.LearningMaterialService;
import com.edumind.backend.service.StudentPortalService;
import com.edumind.backend.service.AssignmentService;
import com.edumind.backend.service.ClassInchargeService;
import com.edumind.backend.service.JwtService;
import com.edumind.backend.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FacultyPortalSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private FacultyPortalService facultyPortalService;

    @MockitoBean
    private LearningMaterialService learningMaterialService;

    @MockitoBean
    private StudentPortalService studentPortalService;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private ClassInchargeService classInchargeService;

    @MockitoBean
    private QuizService quizService;

    @Test
    void facultyCanOnlyRequestProfileForAuthenticatedUsername() throws Exception {
        when(facultyPortalService.getProfile("faculty-alpha"))
                .thenReturn(new FacultyPortalProfileResponse(
                        12L,
                        "EMP-12",
                        "Lecturer",
                        "faculty-alpha",
                        "Faculty Alpha",
                        "alpha@example.test",
                        "Computer Science and Engineering",
                        "CSE"
                ));

        mockMvc.perform(get("/api/faculty/me")
                        .param("username", "faculty-beta")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("faculty-alpha"))
                .andExpect(jsonPath("$.employeeId").value("EMP-12"));

        verify(facultyPortalService).getProfile("faculty-alpha");
    }

    @Test
    void facultyCanOnlyRequestCoursesForAuthenticatedUsername() throws Exception {
        when(facultyPortalService.getAssignedCourses("faculty-alpha"))
                .thenReturn(List.of(new FacultyPortalCourseResponse(
                        31L,
                        "ADS",
                        "Advanced Data Structures",
                        4,
                        5,
                        "B.Tech Computer Science and Engineering",
                        "BTECH-CSE",
                        "2026-27"
                )));

        mockMvc.perform(get("/api/faculty/me/courses")
                        .param("username", "faculty-beta")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ADS"))
                .andExpect(jsonPath("$[0].semesterNumber").value(5));

        verify(facultyPortalService).getAssignedCourses("faculty-alpha");
    }

    @Test
    void adminTokenCannotAccessFacultyPortal() throws Exception {
        mockMvc.perform(get("/api/faculty/me")
                        .header("Authorization", bearerToken("admin", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearerToken("admin", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void legacyAdminTokenCanAccessDashboardUsingCurrentAccountRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearerToken("admin", null)))
                .andExpect(status().isOk());
    }

    @Test
    void facultyCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsPublicForHostingHealthChecks() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void anonymousRequestCannotAccessFacultyPortal() throws Exception {
        mockMvc.perform(get("/api/faculty/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestCannotListLearningMaterials() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/materials"))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultySectionRosterUsesAuthenticatedFacultyIdentity() throws Exception {
        when(classInchargeService.getMySectionStudents(
                "faculty-alpha", 77L, null, 0, 25))
                .thenReturn(new com.edumind.backend.dto.StudentSearchResponse(
                        List.of(), 0, 25, 0, 0));

        mockMvc.perform(get("/api/faculty/me/sections/77/students")
                        .param("username", "faculty-beta")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(classInchargeService).getMySectionStudents(
                "faculty-alpha", 77L, null, 0, 25);
    }

    @Test
    void studentCannotAccessFacultyClassRoster() throws Exception {
        mockMvc.perform(get("/api/faculty/me/sections/77/students")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentEndpointsUseAuthenticatedUsername() throws Exception {
        when(studentPortalService.getProfile("student-alpha"))
                .thenReturn(new com.edumind.backend.dto.StudentPortalProfileResponse(
                        "student-alpha",
                        "Student Alpha",
                        "alpha@example.test",
                        "ST-100",
                        "Computer Science",
                        "CSE",
                        "B.Tech CSE",
                        "BTECH-CSE",
                        3,
                        "2026-27"));

        mockMvc.perform(get("/api/student/me")
                        .param("username", "student-beta")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student-alpha"))
                .andExpect(jsonPath("$.rollNumber").value("ST-100"));

        verify(studentPortalService).getProfile("student-alpha");
    }

    @Test
    void studentCanRequestAuthorizedCoursesAsTheirOwnAccount() throws Exception {
        when(studentPortalService.getCourses("student-alpha"))
                .thenReturn(List.of(new StudentPortalCourseResponse(
                        31L,
                        "DS",
                        "Data Structures",
                        4,
                        3,
                        "B.Tech CSE",
                        "2026-27",
                        List.of("Faculty One"),
                        2L)));

        mockMvc.perform(get("/api/student/me/courses")
                        .param("username", "student-beta")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DS"))
                .andExpect(jsonPath("$[0].learningMaterialCount").value(2));

        verify(studentPortalService).getCourses("student-alpha");
    }

    @Test
    void studentCanListCourseMaterialsAsTheirOwnAccount() throws Exception {
        when(studentPortalService.getCourseMaterials("student-alpha", 31L))
                .thenReturn(List.of(new LearningMaterialResponse(
                        7L,
                        31L,
                        "lecture.pdf",
                        "application/pdf",
                        1024L,
                        Instant.parse("2026-10-10T00:00:00Z"))));

        mockMvc.perform(get("/api/student/courses/31/materials")
                        .param("username", "student-beta")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("lecture.pdf"))
                .andExpect(jsonPath("$[0].size").value(1024));

        verify(studentPortalService).getCourseMaterials("student-alpha", 31L);
    }

    @Test
    void studentCanDownloadCourseMaterialUsingAuthenticatedIdentity() throws Exception {
        LearningMaterialResponse metadata = new LearningMaterialResponse(
                7L,
                31L,
                "lecture.pdf",
                "application/pdf",
                6L,
                Instant.parse("2026-10-10T00:00:00Z"));
        when(learningMaterialService.downloadForStudent("student-alpha", 7L))
                .thenReturn(new LearningMaterialService.Download(
                        metadata, new ByteArrayResource(new byte[] {1, 2, 3, 4, 5, 6})));

        mockMvc.perform(get("/api/student/materials/7/download")
                        .param("username", "student-beta")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Disposition", org.hamcrest.Matchers.containsString(
                                "lecture.pdf")));

        verify(learningMaterialService).downloadForStudent("student-alpha", 7L);
    }

    @Test
    void anonymousRequestCannotAccessStudentPortal() throws Exception {
        mockMvc.perform(get("/api/student/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestCannotDownloadStudentMaterials() throws Exception {
        mockMvc.perform(get("/api/student/materials/7/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsCannotAccessAssignmentApis() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/assignments"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsCannotAccessQuizApis() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/quizzes"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/quizzes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentQuizApiUsesAuthenticatedIdentityAndOmitsAnswerKeys() throws Exception {
        when(quizService.listForStudent("student-alpha"))
                .thenReturn(List.of(new StudentQuizResponse(
                        4L, 31L, "DS", "Data Structures", "Quiz 1", null, null,
                        null, null, null, 1, java.math.BigDecimal.TEN,
                        "AVAILABLE", 0, null, false, List.of(),
                        List.of(new StudentQuizQuestionResponse(
                                9L, "Which structure is FIFO?", java.math.BigDecimal.ONE, 0,
                                List.of(new StudentQuizOptionResponse(21L, "Queue")))))));

        mockMvc.perform(get("/api/student/quizzes")
                        .param("username", "student-beta")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questions[0].options[0].text").value("Queue"))
                .andExpect(jsonPath("$[0].questions[0].options[0].correct").doesNotExist());

        verify(quizService).listForStudent("student-alpha");
    }

    @Test
    void studentCannotAccessFacultyQuizManagement() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/quizzes")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotAccessStudentQuizzes() throws Exception {
        mockMvc.perform(get("/api/student/quizzes")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAccessFacultyAssignmentApis() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/assignments")
                        .header("Authorization", bearerToken("student-alpha", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCannotAccessStudentPortal() throws Exception {
        mockMvc.perform(get("/api/student/me")
                        .header("Authorization", facultyToken("faculty-alpha")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessStudentPortal() throws Exception {
        mockMvc.perform(get("/api/student/me")
                        .header("Authorization", bearerToken("admin", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessFacultyLearningMaterials() throws Exception {
        mockMvc.perform(get("/api/faculty/courses/31/materials")
                        .header("Authorization", bearerToken("admin", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    private String facultyToken(String username) {
        return bearerToken(username, "FACULTY");
    }

    private String bearerToken(String username, String role) {
        return "Bearer " + jwtService.generateToken(username, role);
    }
}
