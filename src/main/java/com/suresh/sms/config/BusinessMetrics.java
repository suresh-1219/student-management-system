package com.suresh.sms.config;

import org.springframework.stereotype.Component;

import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.StudentRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Application-specific numbers next to the built-in JVM / HTTP / database
 * pool metrics: how many students, courses and enrollments exist right now.
 *
 * <p>A gauge is read when metrics are scraped (not on every request), so this
 * costs one cheap COUNT query per row type per scrape. It appears in
 * Prometheus as {@code sms_students}, {@code sms_courses} and
 * {@code sms_enrollments}.
 */
@Component
public class BusinessMetrics implements MeterBinder {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public BusinessMetrics(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository) {

        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {

        Gauge.builder("sms.students", studentRepository, repository -> repository.count())
                .description("Number of students")
                .register(registry);

        Gauge.builder("sms.courses", courseRepository, repository -> repository.count())
                .description("Number of courses")
                .register(registry);

        Gauge.builder("sms.enrollments", enrollmentRepository, repository -> repository.count())
                .description("Number of enrollments")
                .register(registry);
    }
}
