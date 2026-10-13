package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Teacher;
import com.suresh.sms.exception.CourseHasEnrollmentsException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.DuplicateCourseException;
import com.suresh.sms.exception.TeacherNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.TeacherRepository;

@Service
public class CourseService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseRepository repository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {

        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

    /**
     * {@code @Transactional}: {@code convertToDTO} below navigates the
     * course's (possibly already-assigned) lazy {@code teacher} - see the
     * comment on {@code convertToDTO} for why that must happen inside this
     * method's own transaction, not after it returns.
     */
    @Transactional
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

    /**
     * Assigns (or, with a {@code null} id, unassigns) a course's teacher.
     *
     * <p>A dedicated endpoint rather than part of {@link #updateCourse}, so
     * an ordinary "edit the title" request can never accidentally clear or
     * change the assignment - the client has to mean it.
     */
    @Transactional
    public CourseDTO assignTeacher(Long courseId, Long teacherId) {

        Course course = repository.findById(courseId)
                .orElseThrow(() ->
                        new CourseNotFoundException("Course not found with id : " + courseId));

        if (teacherId == null) {
            course.setTeacher(null);
        } else {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new TeacherNotFoundException(
                            "Teacher not found with id : " + teacherId));
            course.setTeacher(teacher);
        }

        Course updated = repository.save(course);

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

    /**
     * Mapped by hand, not left to ModelMapper's implicit nested-property
     * matching: {@code teacher} is a lazy association, and with
     * {@code spring.jpa.open-in-view=false} it can only be safely navigated
     * inside this method's own transaction (see the {@code @Transactional}
     * annotations above) - not later, by a mapping library reflecting over
     * the entity after the session has already closed.
     */
    private CourseDTO convertToDTO(Course course) {

        CourseDTO dto = modelMapper.map(course, CourseDTO.class);

        Teacher teacher = course.getTeacher();

        if (teacher != null) {
            dto.setTeacherId(teacher.getId());
            dto.setTeacherName(teacher.getName());
        }

        return dto;
    }

    private Course convertToEntity(CourseDTO dto) {
        return modelMapper.map(dto, Course.class);
    }
}
