package com.edumind.backend.service;

import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            SemesterRepository semesterRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.semesterRepository = semesterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found with id: " + id));
    }

    @Transactional
    public Student createStudent(
            String username,
            String password,
            String fullName,
            String email,
            String rollNumber,
            Long departmentId,
            Long programId,
            Long semesterId) {

        // Check username
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Check email
        if (email != null && !email.isBlank()
                && userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Check roll number
        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw new RuntimeException("Roll number already exists");
        }

        // Find department
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new RuntimeException("Department not found"));

        // Find program
        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new RuntimeException("Program not found"));

        // Find semester
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() ->
                        new RuntimeException("Semester not found"));

        /*
         * Make sure the selected program belongs
         * to the selected department.
         */
        if (!program.getDepartment().getId().equals(department.getId())) {
            throw new RuntimeException(
                    "Program does not belong to the selected department");
        }

        /*
         * Make sure the selected semester belongs
         * to the selected program.
         */
        if (!semester.getProgram().getId().equals(program.getId())) {
            throw new RuntimeException(
                    "Semester does not belong to the selected program");
        }

        // Create User account
        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setFullName(fullName);
        user.setEmail(email);

        User savedUser = userRepository.save(user);

        // Create Student profile
        Student student = new Student();

        student.setRollNumber(rollNumber);
        student.setUser(savedUser);
        student.setDepartment(department);
        student.setProgram(program);
        student.setSemester(semester);

        return studentRepository.save(student);
    }
    @Transactional
public Student updateStudent(
        Long id,
        String fullName,
        String email,
        String rollNumber,
        Long departmentId,
        Long programId,
        Long semesterId) {

    Student student = studentRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Student not found"));

    if (!student.getRollNumber().equals(rollNumber)
            && studentRepository.existsByRollNumber(rollNumber)) {
        throw new RuntimeException("Roll number already exists");
    }

    User user = student.getUser();

    if (email != null && !email.isBlank()
            && !email.equals(user.getEmail())
            && userRepository.findByEmail(email).isPresent()) {
        throw new RuntimeException("Email already exists");
    }

    Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() ->
                    new RuntimeException("Department not found"));

    Program program = programRepository.findById(programId)
            .orElseThrow(() ->
                    new RuntimeException("Program not found"));

    Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() ->
                    new RuntimeException("Semester not found"));

    if (!program.getDepartment().getId().equals(department.getId())) {
        throw new RuntimeException(
                "Program does not belong to the selected department");
    }

    if (!semester.getProgram().getId().equals(program.getId())) {
        throw new RuntimeException(
                "Semester does not belong to the selected program");
    }

    user.setFullName(fullName);
    user.setEmail(email);

    student.setRollNumber(rollNumber);
    student.setDepartment(department);
    student.setProgram(program);
    student.setSemester(semester);

    userRepository.save(user);

    return studentRepository.save(student);
}

@Transactional
public void deleteStudent(Long id) {

    Student student = studentRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Student not found"));

    User user = student.getUser();

    studentRepository.delete(student);
    userRepository.delete(user);
}
}
