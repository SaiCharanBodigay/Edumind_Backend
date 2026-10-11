package com.edumind.backend;

import com.edumind.backend.repository.ClassInchargeAssignmentRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.SectionRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.service.ClassInchargeService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClassInchargeServiceTests {

    @Test
    void facultyCannotReadRosterForUnassignedSection() {
        ClassInchargeAssignmentRepository assignments =
                mock(ClassInchargeAssignmentRepository.class);
        FacultyRepository faculties = mock(FacultyRepository.class);
        SectionRepository sections = mock(SectionRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        when(assignments.existsByFacultyUserUsernameAndSectionIdAndActiveTrue(
                "faculty-one", 18L)).thenReturn(false);

        ClassInchargeService service = new ClassInchargeService(
                assignments, faculties, sections, students);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMySectionStudents(
                        "faculty-one", 18L, null, 0, 25));

        assertEquals(403, exception.getStatusCode().value());
        verify(assignments).existsByFacultyUserUsernameAndSectionIdAndActiveTrue(
                "faculty-one", 18L);
        verifyNoInteractions(students);
    }
}
