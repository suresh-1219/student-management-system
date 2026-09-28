package com.suresh.sms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.StudentRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;


    @Test
    void reportsRowCountsAsGauges() {

        when(studentRepository.count()).thenReturn(6L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(5L);

        MeterRegistry registry = new SimpleMeterRegistry();

        new BusinessMetrics(studentRepository, courseRepository, enrollmentRepository)
                .bindTo(registry);

        assertEquals(6.0, registry.get("sms.students").gauge().value());
        assertEquals(2.0, registry.get("sms.courses").gauge().value());
        assertEquals(5.0, registry.get("sms.enrollments").gauge().value());
    }

    @Test
    void gaugeIsReadFreshEveryTimeItIsScraped() {

        when(studentRepository.count()).thenReturn(1L, 2L);

        MeterRegistry registry = new SimpleMeterRegistry();

        new BusinessMetrics(studentRepository, courseRepository, enrollmentRepository)
                .bindTo(registry);

        assertEquals(1.0, registry.get("sms.students").gauge().value());
        assertEquals(2.0, registry.get("sms.students").gauge().value());
    }
}
