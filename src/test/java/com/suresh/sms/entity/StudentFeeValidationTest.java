package com.suresh.sms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.suresh.sms.dto.StudentDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Regression test: the DTO (checked at the controller) and the entity (checked
 * by Hibernate on save) used to disagree about fee = 0, which made
 * POST /students with fee 0 pass validation and then fail with HTTP 500.
 */
class StudentFeeValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private boolean dtoRejectsFee(Double fee) {

        StudentDTO dto = new StudentDTO(null, "Suresh", "suresh@gmail.com", "MCA", fee);

        return !validator.validateProperty(dto, "fee").isEmpty();
    }

    private boolean entityRejectsFee(Double fee) {

        Student student = new Student(null, "Suresh", "suresh@gmail.com", "MCA", fee);

        return !validator.validateProperty(student, "fee").isEmpty();
    }

    @Test
    void zeroFeeIsAcceptedEverywhere() {

        assertFalse(dtoRejectsFee(0.0));
        assertFalse(entityRejectsFee(0.0));
    }

    @Test
    void negativeFeeIsRejectedEverywhere() {

        assertTrue(dtoRejectsFee(-1.0));
        assertTrue(entityRejectsFee(-1.0));
    }

    @Test
    void dtoAndEntityAgreeForAllSampleValues() {

        for (double fee : List.of(-5000.0, -0.01, 0.0, 0.01, 1.0, 50000.0)) {

            assertEquals(
                    dtoRejectsFee(fee),
                    entityRejectsFee(fee),
                    "DTO and entity disagree for fee " + fee);
        }
    }
}
