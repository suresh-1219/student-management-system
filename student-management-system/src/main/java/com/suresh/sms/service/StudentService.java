package com.suresh.sms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suresh.sms.entity.Student;
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

    public List<Student> sortStudents(String field) {
        return repository.findAll(Sort.by(Sort.Direction.ASC, field));
    }
    
    public List<Student> sortStudentsDesc(String field) {
        return repository.findAll(Sort.by(Sort.Direction.DESC, field));
    }
    
    public StudentDTO getStudentById(Long id) {

        Student student = repository.findById(id)
                .orElseThrow(() ->
                        new StudentNotFoundException("Student not found with id : " + id));

        return convertToDTO(student);
    }

    public Page<Student> getStudents(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return repository.findAll(pageable);
    } 
    
    public Page<Student> getStudentsWithPaginationAndSorting(int page, int size, String field) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(field));

        return repository.findAll(pageable);
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
    
    public List<Student> getStudentByName(String name) {
        return repository.findByName(name);
    }
    private StudentDTO convertToDTO(Student student) {
        return modelMapper.map(student, StudentDTO.class);
    }
    private Student convertToEntity(StudentDTO dto) {
        return modelMapper.map(dto, Student.class);
    }
}