package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.EnrollmentRequest;
import com.suresh.sms.dto.EnrollmentResponseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.CourseCapacityExceededException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.DuplicateEnrollmentException;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.StudentRepository;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Enrolls a student in a course.
     *
     * <p>{@code @Transactional} matters here: the capacity check (counting
     * existing enrollments) and the save that follows it must run as one
     * unit. {@link CourseRepository#findByIdForUpdate} takes a row lock on
     * the course for the duration of that transaction, so if two requests
     * race for the last seat, the second one waits for the first to
     * commit and then recounts against the up-to-date number - it cannot
     * slip past the capacity check on stale data the way a plain
     * {@code findById} could.
     */
    @Transactional
    public EnrollmentResponseDTO enroll(EnrollmentRequest request) {

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student not found with id : " + request.studentId()));

        Course course = courseRepository.findByIdForUpdate(request.courseId())
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found with id : " + request.courseId()));

        if (repository.existsByStudent_IdAndCourse_Id(student.getId(), course.getId())) {
            throw new DuplicateEnrollmentException(
                    "This student is already enrolled in this course");
        }

        long enrolledCount = repository.countByCourse_Id(course.getId());

        if (enrolledCount >= course.getCapacity()) {
            throw new CourseCapacityExceededException(
                    "This course is full (capacity: " + course.getCapacity() + ")");
        }

        Enrollment saved = repository.save(new Enrollment(student, course));

        return convertToDTO(saved);
    }

    public void unenroll(Long enrollmentId) {

        Enrollment existing = repository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment not found with id : " + enrollmentId));

        repository.delete(existing);
    }

    public List<EnrollmentResponseDTO> getEnrollmentsForStudent(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException("Student not found with id : " + studentId);
        }

        return repository.findByStudent_Id(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<EnrollmentResponseDTO> getEnrollmentsForCourse(Long courseId) {

        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException("Course not found with id : " + courseId);
        }

        return repository.findByCourse_Id(courseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private EnrollmentResponseDTO convertToDTO(Enrollment enrollment) {

        return new EnrollmentResponseDTO(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCode(),
                enrollment.getCourse().getTitle(),
                enrollment.getEnrolledAt()
        );
    }
}
