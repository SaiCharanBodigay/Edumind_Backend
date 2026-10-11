package com.edumind.backend.service;

import com.edumind.backend.dto.AcademicCohortResponse;
import com.edumind.backend.dto.SectionResponse;
import com.edumind.backend.entity.AcademicCohort;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Section;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.repository.AcademicCohortRepository;
import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SectionRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentAcademicPlacementRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AcademicStructureService {

    private final AcademicCohortRepository cohortRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final StudentAcademicPlacementRepository placementRepository;
    private final ClassInchargeAssignmentRepository inchargeRepository;

    public AcademicStructureService(
            AcademicCohortRepository cohortRepository,
            ProgramRepository programRepository,
            SectionRepository sectionRepository,
            SemesterRepository semesterRepository,
            StudentRepository studentRepository,
            StudentAcademicPlacementRepository placementRepository,
            ClassInchargeAssignmentRepository inchargeRepository) {
        this.cohortRepository = cohortRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.placementRepository = placementRepository;
        this.inchargeRepository = inchargeRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademicCohortResponse> getCohorts(Long programId) {
        List<AcademicCohort> cohorts = programId == null
                ? cohortRepository.findAll().stream()
                        .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                        .toList()
                : cohortRepository.findAllByProgramIdOrderByName(programId);
        return cohorts.stream().map(this::toCohortResponse).toList();
    }

    @Transactional
    public AcademicCohortResponse createCohort(String name, Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Program not found."));
        String normalizedName = name.trim();
        if (cohortRepository.existsByProgramIdAndNameIgnoreCase(programId, normalizedName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This cohort already exists for the selected program.");
        }
        return toCohortResponse(cohortRepository.save(
                new AcademicCohort(normalizedName, program)));
    }

    @Transactional
    public void deleteCohort(Long cohortId) {
        AcademicCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Academic cohort not found."));
        if (sectionRepository.existsByCohortId(cohortId)
                || studentRepository.existsByCohortId(cohortId)
                || placementRepository.existsByCohortId(cohortId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a cohort that has sections or current or historical student placements.");
        }
        cohortRepository.delete(cohort);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getSections(
            Long programId,
            Long cohortId,
            Long semesterId) {
        Map<Long, String> activeIncharges = inchargeRepository.findAllActive().stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getSection().getId(),
                        assignment -> assignment.getFaculty().getUser().getFullName(),
                        (first, ignored) -> first));
        return sectionRepository.findFiltered(programId, cohortId, semesterId).stream()
                .map(section -> toSectionResponse(
                        section, activeIncharges.get(section.getId())))
                .toList();
    }

    @Transactional
    public SectionResponse createSection(String name, Long cohortId, Long semesterId) {
        AcademicCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Academic cohort not found."));
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Semester not found."));
        if (!cohort.getProgram().getId().equals(semester.getProgram().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The cohort and semester must belong to the same program.");
        }
        String normalizedName = name.trim();
        if (sectionRepository.existsByCohortIdAndSemesterIdAndNameIgnoreCase(
                cohortId, semesterId, normalizedName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This section already exists for the selected cohort and semester.");
        }
        Section saved = sectionRepository.save(
                new Section(normalizedName, cohort, semester));
        return toSectionResponse(saved, null);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Section not found."));
        if (studentRepository.existsBySectionId(sectionId)
                || placementRepository.existsBySectionId(sectionId)
                || inchargeRepository.existsBySectionId(sectionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a section that has current or historical student placements or class-incharge assignments.");
        }
        sectionRepository.delete(section);
    }

    @Transactional
    public SectionResponse renameSection(Long sectionId, String name) {
        Section section = sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Section not found."));
        String normalizedName = name.trim();
        if (!section.getName().equalsIgnoreCase(normalizedName)
                && sectionRepository.existsByCohortIdAndSemesterIdAndNameIgnoreCase(
                        section.getCohort().getId(),
                        section.getSemester().getId(),
                        normalizedName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This section already exists for the selected cohort and semester.");
        }
        section.rename(normalizedName);
        return toSectionResponse(section, assignmentName(sectionId));
    }

    private AcademicCohortResponse toCohortResponse(AcademicCohort cohort) {
        Program program = cohort.getProgram();
        return new AcademicCohortResponse(
                cohort.getId(),
                cohort.getName(),
                program.getId(),
                program.getName(),
                program.getDepartment().getId(),
                program.getDepartment().getName());
    }

    private SectionResponse toSectionResponse(Section section, String inchargeName) {
        AcademicCohort cohort = section.getCohort();
        Program program = cohort.getProgram();
        Semester semester = section.getSemester();
        return new SectionResponse(
                section.getId(),
                section.getName(),
                cohort.getId(),
                cohort.getName(),
                program.getId(),
                program.getName(),
                program.getDepartment().getId(),
                program.getDepartment().getName(),
                semester.getId(),
                semester.getSemesterNumber(),
                semester.getAcademicYear().getName(),
                inchargeName);
    }

    private String assignmentName(Long sectionId) {
        return inchargeRepository.findFirstBySectionIdAndActiveTrue(sectionId)
                .map(assignment -> assignment.getFaculty().getUser().getFullName())
                .orElse(null);
    }
}
