package com.suresh.sms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.StudentRepository;
import com.suresh.sms.dto.StudentDTO;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

        Student student = convertToEntity(dto);

        Student saved = repository.save(student);

        return convertToDTO(saved);
    }

    public StudentDTO updateStudent(Long id, StudentDTO dto) {

        Student existing = repository.findById(id)
                .orElseThrow(() ->
                        new StudentNotFoundException("Student not found with id : " + id));

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