package com.edumind.backend.service;

import com.edumind.backend.entity.Department;
import com.edumind.backend.entity.AcademicCohort;
import com.edumind.backend.entity.Program;
import com.edumind.backend.entity.Section;
import com.edumind.backend.entity.Semester;
import com.edumind.backend.entity.Student;
import com.edumind.backend.entity.StudentAcademicPlacement;
import com.edumind.backend.entity.User;
import com.edumind.backend.repository.AcademicCohortRepository;
import com.edumind.backend.repository.DepartmentRepository;
import com.edumind.backend.repository.ProgramRepository;
import com.edumind.backend.repository.SectionRepository;
import com.edumind.backend.repository.SemesterRepository;
import com.edumind.backend.repository.StudentAcademicPlacementRepository;
import com.edumind.backend.repository.StudentRepository;
import com.edumind.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final AcademicCohortRepository cohortRepository;
    private final SectionRepository sectionRepository;
    private final StudentAcademicPlacementRepository placementRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            SemesterRepository semesterRepository,
            AcademicCohortRepository cohortRepository,
            SectionRepository sectionRepository,
            StudentAcademicPlacementRepository placementRepository,
            PasswordEncoder passwordEncoder) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.semesterRepository = semesterRepository;
        this.cohortRepository = cohortRepository;
        this.sectionRepository = sectionRepository;
        this.placementRepository = placementRepository;
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
        return createStudent(
                username, password, fullName, email, rollNumber,
                departmentId, programId, semesterId, null, null);
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
            Long semesterId,
            Long cohortId,
            Long sectionId) {

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

        Placement placement = resolvePlacement(
                program, semester, cohortId, sectionId, true);

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
        if (placement != null) {
            student.setCohort(placement.cohort());
            student.setSection(placement.section());
        }

        Student savedStudent = studentRepository.save(student);
        if (placement != null) {
            placementRepository.save(new StudentAcademicPlacement(
                    savedStudent, placement.cohort(), semester, placement.section()));
        }
        return savedStudent;
    }
    @Transactional
public Student updateStudent(
        Long id,
        String fullName,
        String email,
        String rollNumber,
        Long departmentId,
        Long programId,
        Long semesterId,
        Long cohortId,
        Long sectionId) {

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

    Placement placement = resolvePlacement(
            program, semester, cohortId, sectionId, false);

    boolean placementChanged =
            !java.util.Objects.equals(
                    student.getCohort() == null ? null : student.getCohort().getId(),
                    placement.cohort().getId())
            || !java.util.Objects.equals(
                    student.getSection() == null ? null : student.getSection().getId(),
                    placement.section().getId())
            || !student.getSemester().getId().equals(semester.getId());

    user.setFullName(fullName);
    user.setEmail(email);

    student.setRollNumber(rollNumber);
    student.setDepartment(department);
    student.setProgram(program);
    student.setSemester(semester);
    student.setCohort(placement.cohort());
    student.setSection(placement.section());

    userRepository.save(user);

    Student savedStudent = studentRepository.save(student);
    if (placementChanged) {
        placementRepository
                .findFirstByStudentIdAndEndedAtIsNullOrderByStartedAtDesc(savedStudent.getId())
                .ifPresent(currentPlacement -> {
                    currentPlacement.end();
                    placementRepository.save(currentPlacement);
                });
        placementRepository.save(new StudentAcademicPlacement(
                savedStudent, placement.cohort(), semester, placement.section()));
    }
    return savedStudent;
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

    @Transactional
    public void assignPlacement(Long studentId, Long cohortId, Long semesterId, Long sectionId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found."));
        Program program = student.getProgram();
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Semester not found."));
        if (!semester.getProgram().getId().equals(program.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Semester does not belong to the student's program.");
        }
        Placement placement = resolvePlacement(
                program, semester, cohortId, sectionId, false);
        if (java.util.Objects.equals(
                    student.getCohort() == null ? null : student.getCohort().getId(),
                    placement.cohort().getId())
                && java.util.Objects.equals(
                    student.getSection() == null ? null : student.getSection().getId(),
                    placement.section().getId())
                && student.getSemester().getId().equals(semester.getId())) {
            return;
        }
        placementRepository
                .findFirstByStudentIdAndEndedAtIsNullOrderByStartedAtDesc(studentId)
                .ifPresent(currentPlacement -> {
                    currentPlacement.end();
                    placementRepository.save(currentPlacement);
                });
        student.setCohort(placement.cohort());
        student.setSemester(semester);
        student.setSection(placement.section());
        studentRepository.save(student);
        placementRepository.save(new StudentAcademicPlacement(
                student, placement.cohort(), semester, placement.section()));
    }

    private Placement resolvePlacement(
            Program program,
            Semester semester,
            Long cohortId,
            Long sectionId,
            boolean allowUnplaced) {
        if (cohortId == null && sectionId == null && allowUnplaced) {
            return null;
        }
        if (cohortId == null || sectionId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Academic cohort and section are both required.");
        }
        AcademicCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Academic cohort not found."));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Section not found."));
        if (!cohort.getProgram().getId().equals(program.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Academic cohort does not belong to the selected program.");
        }
        if (!section.getCohort().getId().equals(cohort.getId())
                || !section.getSemester().getId().equals(semester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section must belong to the selected cohort and semester.");
        }
        return new Placement(cohort, section);
    }

    private record Placement(AcademicCohort cohort, Section section) {
    }
}
