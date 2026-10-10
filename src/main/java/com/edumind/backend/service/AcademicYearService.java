package com.edumind.backend.service;

import com.edumind.backend.entity.AcademicYear;
import com.edumind.backend.repository.AcademicYearRepository;
import com.edumind.backend.repository.SemesterRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    public AcademicYearService(
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository) {
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
    }

    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findAll();
    }

    public AcademicYear getAcademicYearById(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Academic year not found with id: " + id));
    }

    @Transactional
    public AcademicYear createAcademicYear(String name) {

        if (academicYearRepository.existsByName(name)) {
            throw new RuntimeException(
                    "Academic year already exists");
        }

        AcademicYear academicYear = new AcademicYear();
        academicYear.setName(name);

        return academicYearRepository.save(academicYear);
    }

    @Transactional
    public AcademicYear updateAcademicYear(
            Long id,
            String name) {

        AcademicYear academicYear =
                academicYearRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Academic year not found"));

        if (!academicYear.getName().equals(name)
                && academicYearRepository.existsByName(name)) {

            throw new RuntimeException(
                    "Academic year already exists");
        }

        academicYear.setName(name);

        return academicYearRepository.save(academicYear);
    }

    @Transactional
    public void deleteAcademicYear(Long id) {

        if (!academicYearRepository.existsById(id)) {
            throw new RuntimeException(
                    "Academic year not found");
        }

        if (semesterRepository.existsByAcademicYearId(id)) {
            throw new RuntimeException(
                    "Cannot delete academic year because semesters are assigned to it");
        }

        academicYearRepository.deleteById(id);
    }
}