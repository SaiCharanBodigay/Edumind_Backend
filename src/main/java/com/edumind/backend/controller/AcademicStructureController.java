package com.edumind.backend.controller;

import com.edumind.backend.dto.AcademicCohortRequest;
import com.edumind.backend.dto.AcademicCohortResponse;
import com.edumind.backend.dto.SectionRequest;
import com.edumind.backend.dto.SectionResponse;
import com.edumind.backend.dto.SectionRenameRequest;
import com.edumind.backend.service.AcademicStructureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/admin")
public class AcademicStructureController {

    private final AcademicStructureService academicStructureService;

    public AcademicStructureController(AcademicStructureService academicStructureService) {
        this.academicStructureService = academicStructureService;
    }

    @GetMapping("/cohorts")
    public List<AcademicCohortResponse> getCohorts(
            @RequestParam(required = false) Long programId) {
        return academicStructureService.getCohorts(programId);
    }

    @PostMapping("/cohorts")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicCohortResponse createCohort(
            @Valid @RequestBody AcademicCohortRequest request) {
        return academicStructureService.createCohort(request.name(), request.programId());
    }

    @DeleteMapping("/cohorts/{cohortId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCohort(@PathVariable Long cohortId) {
        academicStructureService.deleteCohort(cohortId);
    }

    @GetMapping("/sections")
    public List<SectionResponse> getSections(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) Long semesterId) {
        return academicStructureService.getSections(programId, cohortId, semesterId);
    }

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@Valid @RequestBody SectionRequest request) {
        return academicStructureService.createSection(
                request.name(), request.cohortId(), request.semesterId());
    }

    @org.springframework.web.bind.annotation.PutMapping("/sections/{sectionId}")
    public SectionResponse renameSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionRenameRequest request) {
        return academicStructureService.renameSection(sectionId, request.name());
    }

    @DeleteMapping("/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable Long sectionId) {
        academicStructureService.deleteSection(sectionId);
    }
}
