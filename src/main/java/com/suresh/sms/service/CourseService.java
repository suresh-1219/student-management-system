package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.exception.CourseHasEnrollmentsException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.DuplicateCourseException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;

@Service
public class CourseService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseRepository repository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    public List<CourseDTO> getAllCourses() {

        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {

        Course course = repository.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException("Course not found with id : " + id));

        return convertToDTO(course);
    }

    public CourseDTO saveCourse(CourseDTO dto) {

        if (repository.existsByCode(dto.getCode())) {
            throw new DuplicateCourseException(
                    "A course with this code already exists");
        }

        Course course = convertToEntity(dto);

        Course saved = repository.save(course);

        return convertToDTO(saved);
    }

    public CourseDTO updateCourse(Long id, CourseDTO dto) {

        Course existing = repository.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException("Course not found with id : " + id));

        if (repository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new DuplicateCourseException(
                    "A course with this code already exists");
        }

        existing.setCode(dto.getCode());
        existing.setTitle(dto.getTitle());
        existing.setCapacity(dto.getCapacity());

        Course updated = repository.save(existing);

        return convertToDTO(updated);
    }

    public void deleteCourse(Long id) {

        Course existing = repository.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException("Course not found with id : " + id));

        if (enrollmentRepository.existsByCourse_Id(id)) {
            throw new CourseHasEnrollmentsException(
                    "Cannot delete a course that still has students enrolled in it. "
                            + "Unenroll every student first.");
        }

        repository.delete(existing);
    }

    private CourseDTO convertToDTO(Course course) {
        return modelMapper.map(course, CourseDTO.class);
    }

    private Course convertToEntity(CourseDTO dto) {
        return modelMapper.map(dto, Course.class);
    }
}
