package com.edumind.backend.service;

import com.edumind.backend.entity.AcademicYear;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.repository.AcademicYearRepository;
import com.edumind.backend.repository.CourseRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public SemesterService(
            SemesterRepository semesterRepository,
            ProgramRepository programRepository,
            AcademicYearRepository academicYearRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository) {

        this.semesterRepository = semesterRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    public List<Semester> getAllSemesters() {
        return semesterRepository.findAll();
    }

    public Semester getSemesterById(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found"));
    }

    @Transactional
    public Semester createSemester(
            Integer semesterNumber,
            Long programId,
            Long academicYearId) {

        if (semesterRepository
                .existsBySemesterNumberAndProgramIdAndAcademicYearId(
                        semesterNumber,
                        programId,
                        academicYearId)) {

            throw new RuntimeException(
                    "Semester already exists for this program and academic year"
            );
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new RuntimeException("Program not found"));

        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() ->
                        new RuntimeException("Academic year not found"));

        Semester semester = new Semester();

        semester.setSemesterNumber(semesterNumber);
        semester.setProgram(program);
        semester.setAcademicYear(academicYear);

        return semesterRepository.save(semester);
    }

    @Transactional
    public Semester updateSemester(
            Long id,
            Integer semesterNumber,
            Long programId,
            Long academicYearId) {

        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Semester not found"));

        if (semesterRepository
                .existsBySemesterNumberAndProgramIdAndAcademicYearId(
                        semesterNumber,
                        programId,
                        academicYearId)) {

            Semester existing = semesterRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Semester not found"));

            if (!existing.getSemesterNumber().equals(semesterNumber)
                    || !existing.getProgram().getId().equals(programId)
                    || !existing.getAcademicYear().getId().equals(academicYearId)) {

                throw new RuntimeException(
                        "Semester already exists for this program and academic year"
                );
            }
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new RuntimeException("Program not found"));

        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() ->
                        new RuntimeException("Academic year not found"));

        semester.setSemesterNumber(semesterNumber);
        semester.setProgram(program);
        semester.setAcademicYear(academicYear);

        return semesterRepository.save(semester);
    }

    @Transactional
    public void deleteSemester(Long id) {

        if (!semesterRepository.existsById(id)) {
            throw new RuntimeException("Semester not found");
        }

        if (courseRepository.existsBySemesterId(id)
                || studentRepository.existsBySemesterId(id)) {
            throw new RuntimeException(
                    "Cannot delete semester because courses or students are assigned to it");
        }

        semesterRepository.deleteById(id);
    }
}
