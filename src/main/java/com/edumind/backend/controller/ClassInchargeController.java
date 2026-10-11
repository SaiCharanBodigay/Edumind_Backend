package com.edumind.backend.controller;

import com.edumind.backend.dto.ClassInchargeRequest;
import com.edumind.backend.dto.ClassInchargeResponse;
import com.edumind.backend.dto.FacultySectionResponse;
import com.edumind.backend.dto.StudentSearchResponse;
import com.edumind.backend.service.ClassInchargeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ClassInchargeController {

    private final ClassInchargeService classInchargeService;

    public ClassInchargeController(ClassInchargeService classInchargeService) {
        this.classInchargeService = classInchargeService;
    }

    @GetMapping("/admin/class-incharges")
    public List<ClassInchargeResponse> getActiveAssignments() {
        return classInchargeService.getActiveAssignments();
    }

    @PostMapping("/admin/class-incharges")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassInchargeResponse assign(@Valid @RequestBody ClassInchargeRequest request) {
        return classInchargeService.assign(
                request.facultyId(), request.sectionId(), request.startDate());
    }

    @DeleteMapping("/admin/class-incharges/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endAssignment(@PathVariable Long assignmentId) {
        classInchargeService.endAssignment(assignmentId);
    }

    @GetMapping("/faculty/me/sections")
    public List<FacultySectionResponse> getMySections(Authentication authentication) {
        return classInchargeService.getMySections(authentication.getName());
    }

    @GetMapping("/faculty/me/sections/{sectionId}/students")
    public StudentSearchResponse getMySectionStudents(
            Authentication authentication,
            @PathVariable Long sectionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        return classInchargeService.getMySectionStudents(
                authentication.getName(), sectionId, search, page, size);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100.");
        }
    }
}
