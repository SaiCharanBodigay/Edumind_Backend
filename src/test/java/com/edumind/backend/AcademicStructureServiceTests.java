package com.edumind.backend;

import com.edumind.backend.entity.AcademicCohort;
import com.edumind.backend.entity.AcademicYear;
import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.repository.AcademicCohortRepository;
import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SectionRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentAcademicPlacementRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.AcademicStructureService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AcademicStructureServiceTests {

    @Test
    void sectionCannotCombineCohortAndSemesterFromDifferentPrograms() {
        var cohorts = mock(AcademicCohortRepository.class);
        var programs = mock(ProgramRepository.class);
        var sections = mock(SectionRepository.class);
        var semesters = mock(SemesterRepository.class);
        var students = mock(StudentRepository.class);
        var placements = mock(StudentAcademicPlacementRepository.class);
        var incharges = mock(ClassInchargeAssignmentRepository.class);

        Program cohortProgram = program(1L);
        Program otherProgram = program(2L);
        AcademicCohort cohort = new AcademicCohort("2025-2029", cohortProgram);
        Semester semester = new Semester(1, otherProgram, new AcademicYear("2026-27"));
        when(cohorts.findById(10L)).thenReturn(Optional.of(cohort));
        when(semesters.findById(20L)).thenReturn(Optional.of(semester));

        AcademicStructureService service = new AcademicStructureService(
                cohorts, programs, sections, semesters, students, placements, incharges);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createSection("A", 10L, 20L));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(sections);
    }

    private Program program(long id) {
        Department department = new Department("Engineering " + id, "ENG" + id);
        department.setId(id);
        Program program = new Program("Program " + id, "P" + id, department);
        program.setId(id);
        return program;
    }
}
