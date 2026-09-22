package com.suresh.sms.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.PageResponse;
import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.entity.Student;
import com.suresh.sms.service.StudentService;

/**
 * Runs the real search (Specifications -> Hibernate -> MySQL) against three
 * seeded students. Every test is rolled back, and every query filters on the
 * unusual "ZzSrch" prefix so other data in the database cannot interfere.
 */
@SpringBootTest
@Transactional
class StudentSearchDatabaseTest {

    private static final String PREFIX = "ZzSrch";

    @Autowired
    private StudentService service;

    @Autowired
    private StudentRepository repository;

    private Student save(String name, String email, String course, String fee) {

        return repository.saveAndFlush(
                new Student(null, name, email, course, new BigDecimal(fee)));
    }

    @BeforeEach
    void seed() {

        save(PREFIX + " Alpha", "zzsrch-alpha@example.com", "ZZ-MCA", "1000.00");
        save(PREFIX + " Beta",  "zzsrch-beta@example.com",  "ZZ-MBA", "2500.50");
        save(PREFIX + " Gamma", "zzsrch-gamma@example.com", "ZZ-MCA", "4000.00");
    }

    private PageResponse<StudentDTO> search(
            String name, String course, String minFee, String maxFee,
            int page, int size, String sort, String direction) {

        return service.searchStudents(
                name,
                course,
                minFee == null ? null : new BigDecimal(minFee),
                maxFee == null ? null : new BigDecimal(maxFee),
                page, size, sort, direction);
    }

    private List<String> names(PageResponse<StudentDTO> result) {

        return result.content().stream().map(StudentDTO::getName).toList();
    }

    @Test
    void nameFilterIsPartialAndCaseInsensitive() {

        PageResponse<StudentDTO> result =
                search("zzsrch", null, null, null, 0, 10, "name", "asc");

        assertEquals(3, result.totalElements());
        assertEquals(
                List.of("ZzSrch Alpha", "ZzSrch Beta", "ZzSrch Gamma"),
                names(result));

        assertEquals(
                List.of("ZzSrch Beta"),
                names(search("ZZSRCH BETA", null, null, null, 0, 10, "name", "asc")));
    }

    @Test
    void feeRangeIsInclusive() {

        PageResponse<StudentDTO> result =
                search("zzsrch", null, "2500.50", "4000", 0, 10, "fee", "asc");

        assertEquals(List.of("ZzSrch Beta", "ZzSrch Gamma"), names(result));
    }

    @Test
    void courseFilterIsExactAndCaseInsensitive() {

        PageResponse<StudentDTO> result =
                search("zzsrch", "zz-mca", null, null, 0, 10, "name", "asc");

        assertEquals(List.of("ZzSrch Alpha", "ZzSrch Gamma"), names(result));
    }

    @Test
    void canSortDescendingByFee() {

        PageResponse<StudentDTO> result =
                search("zzsrch", null, null, null, 0, 10, "fee", "desc");

        assertEquals(
                List.of("ZzSrch Gamma", "ZzSrch Beta", "ZzSrch Alpha"),
                names(result));
    }

    @Test
    void paginationReportsTotalsAndPositions() {

        PageResponse<StudentDTO> first =
                search("zzsrch", null, null, null, 0, 2, "name", "asc");

        assertEquals(2, first.content().size());
        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertTrue(first.first());
        assertFalse(first.last());

        PageResponse<StudentDTO> second =
                search("zzsrch", null, null, null, 1, 2, "name", "asc");

        assertEquals(List.of("ZzSrch Gamma"), names(second));
        assertFalse(second.first());
        assertTrue(second.last());
    }

    @Test
    void likeWildcardsInTheSearchTextAreTreatedAsPlainCharacters() {

        save(PREFIX + " 100% Sure", "zzsrch-pct@example.com", "ZZ-X", "1.00");

        // "%" must not act as "match anything": no name contains "zzsrch %"
        assertEquals(0, search("zzsrch %", null, null, null, 0, 10, "id", "asc").totalElements());

        // "_" must not act as "any single character": "_lpha" must not match "Alpha"
        assertEquals(0, search("zzsrch _lpha", null, null, null, 0, 10, "id", "asc").totalElements());

        // ...but a literal % is found
        assertEquals(1, search("zzsrch 100%", null, null, null, 0, 10, "id", "asc").totalElements());
    }
}
