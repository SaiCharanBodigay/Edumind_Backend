package com.edumind.backend;

import com.edumind.backend.dto.FacultyPortalCourseResponse;
import com.edumind.backend.dto.FacultyPortalProfileResponse;
import com.edumind.backend.service.FacultyPortalService;
import com.edumind.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
    void anonymousRequestCannotAccessFacultyPortal() throws Exception {
        mockMvc.perform(get("/api/faculty/me"))
                .andExpect(status().isForbidden());
    }

    private String facultyToken(String username) {
        return bearerToken(username, "FACULTY");
    }

    private String bearerToken(String username, String role) {
        return "Bearer " + jwtService.generateToken(username, role);
    }
}
