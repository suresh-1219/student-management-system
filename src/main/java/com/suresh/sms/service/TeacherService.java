package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.TeacherDTO;
import com.suresh.sms.entity.Teacher;
import com.suresh.sms.exception.DuplicateTeacherException;
import com.suresh.sms.exception.TeacherHasCoursesException;
import com.suresh.sms.exception.TeacherNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.TeacherRepository;

@Service
public class TeacherService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TeacherRepository repository;

    @Autowired
    private CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<TeacherDTO> getAllTeachers() {

        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherDTO getTeacherById(Long id) {

        Teacher teacher = repository.findById(id)
                .orElseThrow(() ->
                        new TeacherNotFoundException("Teacher not found with id : " + id));

        return convertToDTO(teacher);
    }

    public TeacherDTO saveTeacher(TeacherDTO dto) {

        if (repository.existsByEmail(dto.getEmail())) {
            throw new DuplicateTeacherException(
                    "A teacher with this email already exists");
        }

        Teacher teacher = convertToEntity(dto);

        Teacher saved = repository.save(teacher);

        return convertToDTO(saved);
    }

    public TeacherDTO updateTeacher(Long id, TeacherDTO dto) {

        Teacher existing = repository.findById(id)
                .orElseThrow(() ->
                        new TeacherNotFoundException("Teacher not found with id : " + id));

        if (repository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new DuplicateTeacherException(
                    "A teacher with this email already exists");
        }

        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setDepartment(dto.getDepartment());

        Teacher updated = repository.save(existing);

        return convertToDTO(updated);
    }

    public void deleteTeacher(Long id) {

        Teacher existing = repository.findById(id)
                .orElseThrow(() ->
                        new TeacherNotFoundException("Teacher not found with id : " + id));

        if (courseRepository.existsByTeacher_Id(id)) {
            throw new TeacherHasCoursesException(
                    "Cannot delete a teacher who is still assigned to a course. "
                            + "Unassign them first.");
        }

        repository.delete(existing);
    }

    private TeacherDTO convertToDTO(Teacher teacher) {
        return modelMapper.map(teacher, TeacherDTO.class);
    }

    private Teacher convertToEntity(TeacherDTO dto) {
        return modelMapper.map(dto, Teacher.class);
    }
}
