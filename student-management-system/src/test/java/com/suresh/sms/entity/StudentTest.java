package com.suresh.sms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StudentTest {

    @Test
    void testStudentGettersAndSetters() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(50000.0);

        assertEquals(1L, student.getId());
        assertEquals("Suresh", student.getName());
        assertEquals("suresh@gmail.com", student.getEmail());
        assertEquals("MCA", student.getCourse());
        assertEquals(50000.0, student.getFee());
    }

    @Test
    void testStudentParameterizedConstructor() {

        Student student = new Student(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                50000.0
        );

        assertEquals(1L, student.getId());
        assertEquals("Suresh", student.getName());
        assertEquals("suresh@gmail.com", student.getEmail());
        assertEquals("MCA", student.getCourse());
        assertEquals(50000.0, student.getFee());
    }
}