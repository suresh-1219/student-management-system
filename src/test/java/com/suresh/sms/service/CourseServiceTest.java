package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.exception.CourseHasEnrollmentsException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.DuplicateCourseException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CourseService courseService;

    private Course course(Long id, String code, String title, int capacity) {
        return new Course(id, code, title, capacity);
    }

    private CourseDTO dto(Long id, String code, String title, int capacity) {
        return new CourseDTO(id, code, title, capacity);
    }


    // GET ALL / GET BY ID

    @Test
    void testGetAllCourses() {

        Course c1 = course(1L, "CS101", "Intro to CS", 30);
        CourseDTO d1 = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findAll()).thenReturn(List.of(c1));
        when(modelMapper.map(c1, CourseDTO.class)).thenReturn(d1);

        List<CourseDTO> result = courseService.getAllCourses();

        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCode());
    }

    @Test
    void testGetCourseByIdNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.getCourseById(99L));
    }


    // SAVE

    @Test
    void testSaveCourse() {

        CourseDTO request = dto(null, "CS101", "Intro to CS", 30);
        Course entity = course(null, "CS101", "Intro to CS", 30);
        Course saved = course(1L, "CS101", "Intro to CS", 30);
        CourseDTO response = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.existsByCode("CS101")).thenReturn(false);
        when(modelMapper.map(request, Course.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(modelMapper.map(saved, CourseDTO.class)).thenReturn(response);

        CourseDTO result = courseService.saveCourse(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testSaveCourseRejectsDuplicateCode() {

        CourseDTO request = dto(null, "CS101", "Intro to CS", 30);

        when(repository.existsByCode("CS101")).thenReturn(true);

        assertThrows(
                DuplicateCourseException.class,
                () -> courseService.saveCourse(request));

        verify(repository, never()).save(any(Course.class));
    }


    // UPDATE

    @Test
    void testUpdateCourseRejectsCodeOfAnotherCourse() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        CourseDTO request = dto(null, "CS999", "Renamed", 40);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("CS999", 1L)).thenReturn(true);

        assertThrows(
                DuplicateCourseException.class,
                () -> courseService.updateCourse(1L, request));

        verify(repository, never()).save(any(Course.class));
    }


    // DELETE

    @Test
    void testDeleteCourseNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.deleteCourse(99L));
    }

    @Test
    void testDeleteCourseRejectsWhenStudentsAreEnrolled() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsByCourse_Id(1L)).thenReturn(true);

        assertThrows(
                CourseHasEnrollmentsException.class,
                () -> courseService.deleteCourse(1L));

        verify(repository, never()).delete(any(Course.class));
    }

    @Test
    void testDeleteCourseSucceedsWhenNoEnrollments() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsByCourse_Id(1L)).thenReturn(false);

        courseService.deleteCourse(1L);

        verify(repository).delete(existing);
    }
}
