package com.edumind.backend.service;

import com.edumind.backend.entity.Department;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.FacultyRepository;
import com.edumind.backend.repository.ProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final FacultyRepository facultyRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            FacultyRepository facultyRepository) {

        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.facultyRepository = facultyRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Transactional
    public Department createDepartment(String name, String code) {

        if (departmentRepository.existsByCode(code)) {
            throw new RuntimeException("Department code already exists");
        }

        if (departmentRepository.existsByName(name)) {
            throw new RuntimeException("Department name already exists");
        }

        Department department = new Department();
        department.setName(name);
        department.setCode(code);

        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, String name, String code) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (!department.getCode().equals(code)
                && departmentRepository.existsByCode(code)) {
            throw new RuntimeException("Department code already exists");
        }

        if (!department.getName().equals(name)
                && departmentRepository.existsByName(name)) {
            throw new RuntimeException("Department name already exists");
        }

        department.setName(name);
        department.setCode(code);

        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {

        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("Department not found");
        }

        if (programRepository.existsByDepartmentId(id)) {
            throw new RuntimeException(
                    "Cannot delete department because programs are assigned to it"
            );
        }

        if (facultyRepository.existsByDepartmentId(id)) {
            throw new RuntimeException(
                    "Cannot delete department because faculty are assigned to it"
            );
        }

        departmentRepository.deleteById(id);
    }
}