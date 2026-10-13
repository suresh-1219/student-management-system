package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.suresh.sms.dto.TeacherDTO;
import com.suresh.sms.entity.Teacher;
import com.suresh.sms.exception.DuplicateTeacherException;
import com.suresh.sms.exception.TeacherHasCoursesException;
import com.suresh.sms.exception.TeacherNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TeacherRepository repository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TeacherService teacherService;

    private Teacher teacher(Long id, String name, String email, String department) {
        return new Teacher(id, name, email, department);
    }

    private TeacherDTO dto(Long id, String name, String email, String department) {
        return new TeacherDTO(id, name, email, department);
    }


    // GET ALL / GET BY ID

    @Test
    void testGetAllTeachers() {

        Teacher t1 = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");
        TeacherDTO d1 = dto(1L, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.findAll()).thenReturn(List.of(t1));
        when(modelMapper.map(t1, TeacherDTO.class)).thenReturn(d1);

        List<TeacherDTO> result = teacherService.getAllTeachers();

        assertEquals(1, result.size());
        assertEquals("Dr. Rao", result.get(0).getName());
    }

    @Test
    void testGetTeacherByIdNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TeacherNotFoundException.class,
                () -> teacherService.getTeacherById(99L));
    }

    @Test
    void testGetTeacherByIdFound() {

        Teacher t1 = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");
        TeacherDTO d1 = dto(1L, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.findById(1L)).thenReturn(Optional.of(t1));
        when(modelMapper.map(t1, TeacherDTO.class)).thenReturn(d1);

        TeacherDTO result = teacherService.getTeacherById(1L);

        assertEquals("rao@school.edu", result.getEmail());
    }


    // SAVE (CREATE)

    @Test
    void testSaveTeacherDuplicateEmail() {

        TeacherDTO dto = dto(null, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.existsByEmail("rao@school.edu")).thenReturn(true);

        assertThrows(
                DuplicateTeacherException.class,
                () -> teacherService.saveTeacher(dto));

        verify(repository, never()).save(any());
    }

    @Test
    void testSaveTeacherSuccess() {

        TeacherDTO dto = dto(null, "Dr. Rao", "rao@school.edu", "Mathematics");
        Teacher entity = teacher(null, "Dr. Rao", "rao@school.edu", "Mathematics");
        Teacher saved = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");
        TeacherDTO savedDto = dto(1L, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.existsByEmail("rao@school.edu")).thenReturn(false);
        when(modelMapper.map(dto, Teacher.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(modelMapper.map(saved, TeacherDTO.class)).thenReturn(savedDto);

        TeacherDTO result = teacherService.saveTeacher(dto);

        assertEquals(1L, result.getId());
    }


    // UPDATE

    @Test
    void testUpdateTeacherNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TeacherNotFoundException.class,
                () -> teacherService.updateTeacher(99L, dto(null, "x", "x@x.com", null)));
    }

    @Test
    void testUpdateTeacherDuplicateEmailExcludesSelf() {

        Teacher existing = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");
        TeacherDTO update = dto(null, "Dr. Rao", "newmail@school.edu", "Physics");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("newmail@school.edu", 1L)).thenReturn(true);

        assertThrows(
                DuplicateTeacherException.class,
                () -> teacherService.updateTeacher(1L, update));

        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateTeacherSuccess() {

        Teacher existing = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");
        TeacherDTO update = dto(null, "Dr. Rao", "rao@school.edu", "Physics");
        TeacherDTO updatedDto = dto(1L, "Dr. Rao", "rao@school.edu", "Physics");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("rao@school.edu", 1L)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, TeacherDTO.class)).thenReturn(updatedDto);

        TeacherDTO result = teacherService.updateTeacher(1L, update);

        assertEquals("Physics", result.getDepartment());
    }


    // DELETE

    @Test
    void testDeleteTeacherNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TeacherNotFoundException.class,
                () -> teacherService.deleteTeacher(99L));
    }

    @Test
    void testDeleteTeacherStillAssignedToCourse() {

        Teacher existing = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByTeacher_Id(1L)).thenReturn(true);

        assertThrows(
                TeacherHasCoursesException.class,
                () -> teacherService.deleteTeacher(1L));

        verify(repository, never()).delete(any());
    }

    @Test
    void testDeleteTeacherSuccess() {

        Teacher existing = teacher(1L, "Dr. Rao", "rao@school.edu", "Mathematics");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByTeacher_Id(1L)).thenReturn(false);

        teacherService.deleteTeacher(1L);

        verify(repository).delete(existing);
    }
}
