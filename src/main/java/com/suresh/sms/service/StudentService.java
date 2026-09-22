package com.suresh.sms.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.DuplicateStudentException;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.StudentRepository;
import com.suresh.sms.repository.StudentSpecifications;
import com.suresh.sms.dto.PageResponse;
import com.suresh.sms.dto.StudentDTO;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class StudentService {

    /** Fields a client may sort by. Anything else is rejected with HTTP 400. */
    private static final List<String> SORTABLE_FIELDS =
            List.of("id", "name", "email", "course", "fee");

    static final int MAX_PAGE_SIZE = 100;

	@Autowired
	private ModelMapper modelMapper;

    @Autowired
    private StudentRepository repository;

    public List<StudentDTO> getAllStudents() {

        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public StudentDTO getStudentById(Long id) {

        Student student = repository.findById(id)
                .orElseThrow(() ->
                        new StudentNotFoundException("Student not found with id : " + id));

        return convertToDTO(student);
    }

    public List<StudentDTO> sortStudents(String field) {
        validateSortField(field);
        return repository.findAll(Sort.by(Sort.Direction.ASC, field))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentDTO> sortStudentsDesc(String field) {
        validateSortField(field);
        return repository.findAll(Sort.by(Sort.Direction.DESC, field))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentDTO> getStudentByName(String name) {
        return repository.findByName(name)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<StudentDTO> getStudents(int page, int size) {

        validatePaging(page, size);

        Pageable pageable = PageRequest.of(page, size);

        return repository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<StudentDTO> getStudentsWithPaginationAndSorting(int page, int size, String field) {

        validatePaging(page, size);
        validateSortField(field);

        Pageable pageable = PageRequest.of(page, size, Sort.by(field));

        return repository.findAll(pageable)
                .map(this::convertToDTO);
    }
    
    public StudentDTO saveStudent(StudentDTO dto) {

        if (repository.existsByEmail(dto.getEmail())) {
            throw new DuplicateStudentException(
                    "A student with this email already exists");
        }

        Student student = convertToEntity(dto);

        Student saved = repository.save(student);

        return convertToDTO(saved);
    }

    public StudentDTO updateStudent(Long id, StudentDTO dto) {

        Student existing = repository.findById(id)
                .orElseThrow(() ->
                        new StudentNotFoundException("Student not found with id : " + id));

        if (repository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new DuplicateStudentException(
                    "A student with this email already exists");
        }

        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setCourse(dto.getCourse());
        existing.setFee(dto.getFee());

        Student updated = repository.save(existing);

        return convertToDTO(updated);
    }
    public void deleteStudent(Long id) {

        Student existing = repository.findById(id)
                .orElseThrow(() ->
                    new StudentNotFoundException("Student not found with id : " + id));

        repository.delete(existing);
    }
    
    /**
     * The one search endpoint: optional filters (name, course, fee range),
     * sorting and paging in a single call.
     */
    public PageResponse<StudentDTO> searchStudents(
            String name,
            String course,
            BigDecimal minFee,
            BigDecimal maxFee,
            int page,
            int size,
            String sortField,
            String direction) {

        validatePaging(page, size);
        validateSortField(sortField);
        Sort.Direction sortDirection = parseDirection(direction);

        if (minFee != null && maxFee != null && minFee.compareTo(maxFee) > 0) {
            throw new InvalidRequestException("minFee cannot be greater than maxFee");
        }

        Specification<Student> spec = (root, query, cb) -> cb.conjunction();

        if (name != null && !name.isBlank()) {
            spec = spec.and(StudentSpecifications.nameContains(name.trim()));
        }

        if (course != null && !course.isBlank()) {
            spec = spec.and(StudentSpecifications.courseEquals(course.trim()));
        }

        if (minFee != null) {
            spec = spec.and(StudentSpecifications.feeAtLeast(minFee));
        }

        if (maxFee != null) {
            spec = spec.and(StudentSpecifications.feeAtMost(maxFee));
        }

        // Tie-break on id so paging is stable when many rows share the same value.
        Sort sort = Sort.by(sortDirection, sortField);
        if (!"id".equals(sortField)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        return PageResponse.from(
                repository.findAll(spec, pageable).map(this::convertToDTO));
    }

    private Sort.Direction parseDirection(String direction) {

        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }

        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }

        throw new InvalidRequestException("Invalid direction. Allowed values: asc, desc");
    }

    private void validateSortField(String field) {

        if (field == null || !SORTABLE_FIELDS.contains(field)) {
            throw new InvalidRequestException(
                    "Invalid sort field. Allowed fields: "
                            + String.join(", ", SORTABLE_FIELDS));
        }
    }

    private void validatePaging(int page, int size) {

        if (page < 0) {
            throw new InvalidRequestException("Page index must not be negative");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private StudentDTO convertToDTO(Student student) {
        return modelMapper.map(student, StudentDTO.class);
    }
    private Student convertToEntity(StudentDTO dto) {
        return modelMapper.map(dto, Student.class);
    }
}