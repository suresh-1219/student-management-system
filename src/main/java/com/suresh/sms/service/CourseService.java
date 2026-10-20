package com.suresh.sms.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.dto.SyllabusDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Teacher;
import com.suresh.sms.exception.CourseHasEnrollmentsException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.CourseSyllabusNotFoundException;
import com.suresh.sms.exception.DuplicateCourseException;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.StorageException;
import com.suresh.sms.exception.TeacherNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.TeacherRepository;

@Service
public class CourseService {

    // Deliberately narrow: a syllabus is reference material, not something
    // the API should accept arbitrary executables or scripts as.
    private static final Set<String> ALLOWED_SYLLABUS_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private static final long MAX_SYLLABUS_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseRepository repository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StorageService storageService;

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


    // SYLLABUS (file attachment, stored in S3-compatible object storage - see StorageService)

    /**
     * Uploads (or replaces) a course's syllabus file. A previous syllabus,
     * if any, is deleted from storage first, so re-uploading never leaves
     * an orphaned object behind.
     */
    @Transactional
    public CourseDTO uploadSyllabus(Long courseId, MultipartFile file) {

        Course course = repository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found with id : " + courseId));

        validateSyllabusFile(file);

        if (course.hasSyllabus()) {
            storageService.delete(course.getSyllabusObjectKey());
        }

        String objectKey = "course-syllabi/" + courseId + "/" + UUID.randomUUID();

        try (InputStream data = file.getInputStream()) {
            storageService.upload(objectKey, data, file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new StorageException("Failed to read uploaded syllabus file", ex);
        }

        course.setSyllabusObjectKey(objectKey);
        course.setSyllabusOriginalFilename(file.getOriginalFilename());
        course.setSyllabusContentType(file.getContentType());
        course.setSyllabusSizeBytes(file.getSize());

        Course updated = repository.save(course);

        return convertToDTO(updated);
    }

    @Transactional(readOnly = true)
    public SyllabusDTO getSyllabusDownload(Long courseId) {

        Course course = repository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found with id : " + courseId));

        if (!course.hasSyllabus()) {
            throw new CourseSyllabusNotFoundException(
                    "Course " + courseId + " has no syllabus uploaded");
        }

        String url = storageService.presignedDownloadUrl(course.getSyllabusObjectKey());

        return new SyllabusDTO(
                course.getSyllabusOriginalFilename(),
                course.getSyllabusSizeBytes(),
                url,
                storageService.getPresignedUrlExpirySeconds());
    }

    @Transactional
    public void deleteSyllabus(Long courseId) {

        Course course = repository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found with id : " + courseId));

        if (!course.hasSyllabus()) {
            throw new CourseSyllabusNotFoundException(
                    "Course " + courseId + " has no syllabus uploaded");
        }

        storageService.delete(course.getSyllabusObjectKey());

        course.setSyllabusObjectKey(null);
        course.setSyllabusOriginalFilename(null);
        course.setSyllabusContentType(null);
        course.setSyllabusSizeBytes(null);

        repository.save(course);
    }

    private void validateSyllabusFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Syllabus file must not be empty");
        }

        if (file.getSize() > MAX_SYLLABUS_SIZE_BYTES) {
            throw new InvalidRequestException(
                    "Syllabus file exceeds the 10 MB limit");
        }

        if (!ALLOWED_SYLLABUS_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException(
                    "Syllabus must be a PDF, Word or PowerPoint document");
        }
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

        if (course.hasSyllabus()) {
            dto.setSyllabusFilename(course.getSyllabusOriginalFilename());
            dto.setSyllabusSizeBytes(course.getSyllabusSizeBytes());
        }

        return dto;
    }

    private Course convertToEntity(CourseDTO dto) {
        return modelMapper.map(dto, Course.class);
    }
}
