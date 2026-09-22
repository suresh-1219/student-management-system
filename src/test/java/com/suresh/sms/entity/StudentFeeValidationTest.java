package com.suresh.sms.entity;

import java.math.BigDecimal;
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

    private boolean dtoRejectsFee(BigDecimal fee) {

        StudentDTO dto = new StudentDTO(null, "Suresh", "suresh@gmail.com", "MCA", fee);

        return !validator.validateProperty(dto, "fee").isEmpty();
    }

    private boolean entityRejectsFee(BigDecimal fee) {

        Student student = new Student(null, "Suresh", "suresh@gmail.com", "MCA", fee);

        return !validator.validateProperty(student, "fee").isEmpty();
    }

    @Test
    void zeroFeeIsAcceptedEverywhere() {

        assertFalse(dtoRejectsFee(BigDecimal.valueOf(0.0)));
        assertFalse(entityRejectsFee(BigDecimal.valueOf(0.0)));
    }

    @Test
    void negativeFeeIsRejectedEverywhere() {

        assertTrue(dtoRejectsFee(BigDecimal.valueOf(-1.0)));
        assertTrue(entityRejectsFee(BigDecimal.valueOf(-1.0)));
    }

    @Test
    void dtoAndEntityAgreeForAllSampleValues() {

        for (BigDecimal fee : List.of(BigDecimal.valueOf(-5000.0), BigDecimal.valueOf(-0.01), BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.01), BigDecimal.valueOf(1.0), BigDecimal.valueOf(50000.0))) {

            assertEquals(
                    dtoRejectsFee(fee),
                    entityRejectsFee(fee),
                    "DTO and entity disagree for fee " + fee);
        }
    }

    @Test
    void moreThanTwoDecimalPlacesAreRejectedEverywhere() {

        BigDecimal tooPrecise = new BigDecimal("10.999");

        assertTrue(dtoRejectsFee(tooPrecise));
        assertTrue(entityRejectsFee(tooPrecise));

        // exactly two decimals is fine
        assertFalse(dtoRejectsFee(new BigDecimal("10.99")));
        assertFalse(entityRejectsFee(new BigDecimal("10.99")));
    }

    @Test
    void moreThanTenDigitsBeforeTheDecimalPointAreRejectedEverywhere() {

        BigDecimal tooBig = new BigDecimal("12345678901");   // 11 digits
        BigDecimal maxOk  = new BigDecimal("9999999999.99"); // 10 digits + 2 decimals

        assertTrue(dtoRejectsFee(tooBig));
        assertTrue(entityRejectsFee(tooBig));

        assertFalse(dtoRejectsFee(maxOk));
        assertFalse(entityRejectsFee(maxOk));
    }
}
