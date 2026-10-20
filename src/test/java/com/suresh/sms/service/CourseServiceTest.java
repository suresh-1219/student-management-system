package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.mock.web.MockMultipartFile;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.dto.SyllabusDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Teacher;
import com.suresh.sms.exception.CourseHasEnrollmentsException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.CourseSyllabusNotFoundException;
import com.suresh.sms.exception.DuplicateCourseException;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.TeacherNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private StorageService storageService;

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

    @Test
    void testGetCourseByIdPopulatesTeacherFieldsWhenAssigned() {

        // Regression test for the lazy-loading fix: convertToDTO must read
        // the teacher explicitly (course.getTeacher()) rather than relying
        // on ModelMapper's implicit nested-property mapping, which would
        // risk a LazyInitializationException outside a transaction with
        // spring.jpa.open-in-view=false. This confirms the DTO actually
        // ends up carrying teacherId/teacherName once that wiring is in
        // place; see CourseServiceIT for the real Hibernate-proxy case.
        Course existing = course(1L, "CS101", "Intro to CS", 30);
        Teacher teacher = new Teacher(5L, "Dr. Rao", "rao@school.edu", "Mathematics");
        existing.setTeacher(teacher);

        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        CourseDTO result = courseService.getCourseById(1L);

        assertEquals(5L, result.getTeacherId());
        assertEquals("Dr. Rao", result.getTeacherName());
    }

    @Test
    void testGetCourseByIdLeavesTeacherFieldsNullWhenUnassigned() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        CourseDTO result = courseService.getCourseById(1L);

        assertEquals(null, result.getTeacherId());
        assertEquals(null, result.getTeacherName());
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


    // ASSIGN TEACHER

    @Test
    void testAssignTeacherCourseNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.assignTeacher(99L, 5L));
    }

    @Test
    void testAssignTeacherRejectsUnknownTeacher() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TeacherNotFoundException.class,
                () -> courseService.assignTeacher(1L, 99L));

        verify(repository, never()).save(any(Course.class));
    }

    @Test
    void testAssignTeacherSuccess() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        Teacher teacher = new Teacher(5L, "Dr. Rao", "rao@school.edu", "Mathematics");
        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findById(5L)).thenReturn(Optional.of(teacher));
        when(repository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        CourseDTO result = courseService.assignTeacher(1L, 5L);

        assertEquals(teacher, existing.getTeacher());
        assertEquals(5L, result.getTeacherId());
        assertEquals("Dr. Rao", result.getTeacherName());
    }

    @Test
    void testAssignTeacherWithNullIdUnassigns() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        existing.setTeacher(new Teacher(5L, "Dr. Rao", "rao@school.edu", "Mathematics"));
        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        CourseDTO result = courseService.assignTeacher(1L, null);

        assertEquals(null, existing.getTeacher());
        assertEquals(null, result.getTeacherId());
        assertEquals(null, result.getTeacherName());

        verify(teacherRepository, never()).findById(any());
    }


    // UPLOAD SYLLABUS

    @Test
    void testUploadSyllabusCourseNotFound() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "content".getBytes());

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.uploadSyllabus(99L, file));
    }

    @Test
    void testUploadSyllabusRejectsEmptyFile() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        MockMultipartFile empty = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", new byte[0]);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidRequestException.class,
                () -> courseService.uploadSyllabus(1L, empty));

        verify(storageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void testUploadSyllabusRejectsDisallowedContentType() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        MockMultipartFile exe = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "bad".getBytes());

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidRequestException.class,
                () -> courseService.uploadSyllabus(1L, exe));

        verify(storageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void testUploadSyllabusRejectsOversizedFile() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        byte[] tooBig = new byte[11 * 1024 * 1024]; // 11 MB > the 10 MB limit
        MockMultipartFile big = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", tooBig);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                InvalidRequestException.class,
                () -> courseService.uploadSyllabus(1L, big));

        verify(storageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void testUploadSyllabusSucceeds() throws Exception {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "content".getBytes());
        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        CourseDTO result = courseService.uploadSyllabus(1L, file);

        verify(storageService).upload(anyString(), any(), eq(7L), eq("application/pdf"));
        assertNotNull(existing.getSyllabusObjectKey());
        assertEquals("syllabus.pdf", existing.getSyllabusOriginalFilename());
        assertEquals("syllabus.pdf", result.getSyllabusFilename());
        assertEquals(7L, result.getSyllabusSizeBytes());
    }

    @Test
    void testUploadSyllabusReplacingAnExistingOneDeletesTheOldObjectFirst() throws Exception {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        existing.setSyllabusObjectKey("course-syllabi/1/old-key");
        existing.setSyllabusOriginalFilename("old.pdf");
        existing.setSyllabusSizeBytes(3L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "new.pdf", "application/pdf", "newcontent".getBytes());
        CourseDTO mapped = dto(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, CourseDTO.class)).thenReturn(mapped);

        courseService.uploadSyllabus(1L, file);

        verify(storageService).delete("course-syllabi/1/old-key");
        verify(storageService).upload(anyString(), any(), eq(10L), eq("application/pdf"));
        assertEquals("new.pdf", existing.getSyllabusOriginalFilename());
    }


    // GET SYLLABUS DOWNLOAD

    @Test
    void testGetSyllabusDownloadCourseNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.getSyllabusDownload(99L));
    }

    @Test
    void testGetSyllabusDownloadThrowsWhenNoneUploaded() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                CourseSyllabusNotFoundException.class,
                () -> courseService.getSyllabusDownload(1L));
    }

    @Test
    void testGetSyllabusDownloadReturnsPresignedUrl() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        existing.setSyllabusObjectKey("course-syllabi/1/key");
        existing.setSyllabusOriginalFilename("syllabus.pdf");
        existing.setSyllabusSizeBytes(123L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(storageService.presignedDownloadUrl("course-syllabi/1/key"))
                .thenReturn("https://objectstore.local/presigned");
        when(storageService.getPresignedUrlExpirySeconds()).thenReturn(600);

        SyllabusDTO result = courseService.getSyllabusDownload(1L);

        assertEquals("syllabus.pdf", result.getFilename());
        assertEquals(123L, result.getSizeBytes());
        assertEquals("https://objectstore.local/presigned", result.getDownloadUrl());
        assertEquals(600, result.getExpiresInSeconds());
    }


    // DELETE SYLLABUS

    @Test
    void testDeleteSyllabusCourseNotFound() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> courseService.deleteSyllabus(99L));
    }

    @Test
    void testDeleteSyllabusThrowsWhenNoneUploaded() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                CourseSyllabusNotFoundException.class,
                () -> courseService.deleteSyllabus(1L));

        verify(storageService, never()).delete(anyString());
    }

    @Test
    void testDeleteSyllabusSucceeds() {

        Course existing = course(1L, "CS101", "Intro to CS", 30);
        existing.setSyllabusObjectKey("course-syllabi/1/key");
        existing.setSyllabusOriginalFilename("syllabus.pdf");
        existing.setSyllabusContentType("application/pdf");
        existing.setSyllabusSizeBytes(123L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        courseService.deleteSyllabus(1L);

        verify(storageService).delete("course-syllabi/1/key");
        assertEquals(null, existing.getSyllabusObjectKey());
        assertEquals(null, existing.getSyllabusOriginalFilename());
        assertEquals(null, existing.getSyllabusContentType());
        assertEquals(null, existing.getSyllabusSizeBytes());
        verify(repository).save(existing);
    }
}
