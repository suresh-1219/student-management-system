package com.suresh.sms.dto;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StudentDTOTest {

    @Test
    void testDefaultConstructor() {
        StudentDTO student = new StudentDTO();

        assertNotNull(student);
    }

    @Test
    void testParameterizedConstructor() {
        StudentDTO student = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        assertEquals(1L, student.getId());
        assertEquals("Suresh", student.getName());
        assertEquals("suresh@gmail.com", student.getEmail());
        assertEquals("MCA", student.getCourse());
        assertEquals(BigDecimal.valueOf(50000.0), student.getFee());
    }

    @Test
    void testGettersAndSetters() {
        StudentDTO student = new StudentDTO();

        student.setId(2L);
        student.setName("Ravi");
        student.setEmail("ravi@gmail.com");
        student.setCourse("Java");
        student.setFee(BigDecimal.valueOf(30000.0));

        assertEquals(2L, student.getId());
        assertEquals("Ravi", student.getName());
        assertEquals("ravi@gmail.com", student.getEmail());
        assertEquals("Java", student.getCourse());
        assertEquals(BigDecimal.valueOf(30000.0), student.getFee());
    }
}