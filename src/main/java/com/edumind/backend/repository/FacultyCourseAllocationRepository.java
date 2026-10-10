package com.edumind.backend.repository;

import com.edumind.backend.entity.FacultyCourseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacultyCourseAllocationRepository
        extends JpaRepository<FacultyCourseAllocation, Long> {

    boolean existsByFacultyIdAndCourseId(Long facultyId, Long courseId);
    boolean existsByFacultyId(Long facultyId);
    List<FacultyCourseAllocation> findByFacultyId(Long facultyId);

    List<FacultyCourseAllocation> findByCourseId(Long courseId);

    @Query("""
            select allocation
            from FacultyCourseAllocation allocation
            join fetch allocation.course course
            join fetch course.semester semester
            join fetch semester.program
            join fetch semester.academicYear
            where allocation.faculty.user.username = :username
            order by course.code
            """)
    List<FacultyCourseAllocation> findPortalCoursesByFacultyUsername(
            @Param("username") String username);
}
