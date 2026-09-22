package com.suresh.sms.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.suresh.sms.entity.Student;

/**
 * Building blocks for the student search. Each method returns one filter;
 * StudentService combines the ones the caller asked for with AND.
 */
public final class StudentSpecifications {

    /** Escape character used with LIKE (not a backslash, so it is safe on every database). */
    private static final char ESCAPE = '!';

    private StudentSpecifications() {
    }

    /** Case-insensitive "contains" match on the name. */
    public static Specification<Student> nameContains(String name) {

        String pattern = "%" + escapeLike(name.toLowerCase()) + "%";

        return (root, query, cb) ->
                cb.like(cb.lower(root.<String>get("name")), pattern, ESCAPE);
    }

    /** Case-insensitive exact match on the course. */
    public static Specification<Student> courseEquals(String course) {

        String value = course.toLowerCase();

        return (root, query, cb) ->
                cb.equal(cb.lower(root.<String>get("course")), value);
    }

    public static Specification<Student> feeAtLeast(BigDecimal min) {

        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.<BigDecimal>get("fee"), min);
    }

    public static Specification<Student> feeAtMost(BigDecimal max) {

        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.<BigDecimal>get("fee"), max);
    }

    /**
     * Makes the characters that mean something in LIKE (% and _) match
     * literally, so a search for "100%" finds "100%" and not everything.
     */
    static String escapeLike(String text) {

        return text
                .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
