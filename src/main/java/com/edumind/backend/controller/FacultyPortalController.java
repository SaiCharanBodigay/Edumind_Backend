package com.edumind.backend.controller;

import com.edumind.backend.dto.FacultyPortalCourseResponse;
import com.edumind.backend.dto.FacultyPortalProfileResponse;
import com.edumind.backend.service.FacultyPortalService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyPortalController {

    private final FacultyPortalService facultyPortalService;

    public FacultyPortalController(FacultyPortalService facultyPortalService) {
        this.facultyPortalService = facultyPortalService;
    }

    @GetMapping("/me")
    public FacultyPortalProfileResponse getMyProfile(
            Authentication authentication) {
        return facultyPortalService.getProfile(authentication.getName());
    }

    @GetMapping("/me/courses")
    public List<FacultyPortalCourseResponse> getMyCourses(
            Authentication authentication) {
        return facultyPortalService.getAssignedCourses(authentication.getName());
    }
}
