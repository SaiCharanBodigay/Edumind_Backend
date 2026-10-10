package com.edumind.backend.service;

import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Program;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.ProgramRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;

    public ProgramService(
            ProgramRepository programRepository,
            DepartmentRepository departmentRepository) {

        this.programRepository = programRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public Program getProgramById(Long id) {

        return programRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Program not found"
                        ));
    }

    public Program createProgram(
            String name,
            String code,
            Long departmentId) {

        if (programRepository.existsByCode(code)) {
            throw new RuntimeException(
                    "Program code already exists"
            );
        }

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"
                                ));

        Program program =
                new Program(name, code, department);

        return programRepository.save(program);
    }

    public Program updateProgram(
            Long id,
            String name,
            String code,
            Long departmentId) {

        Program program = getProgramById(id);

        if (!program.getCode().equals(code)
                && programRepository.existsByCode(code)) {
            throw new RuntimeException(
                    "Program code already exists"
            );
        }

        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"
                                ));

        program.setName(name);
        program.setCode(code);
        program.setDepartment(department);

        return programRepository.save(program);
    }

    public void deleteProgram(Long id) {

        Program program = getProgramById(id);

        programRepository.delete(program);
    }
}
