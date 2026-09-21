package com.suresh.sms.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.entity.Student;

/**
 * Talks to the REAL database (the one configured in application.properties)
 * to prove the Flyway schema and the JPA mapping agree.
 *
 * Every test runs inside a transaction that is rolled back at the end, so no
 * test data is left behind.
 */
@SpringBootTest
@Transactional
class StudentRepositoryDatabaseTest {

    @Autowired
    private StudentRepository repository;

    private Student student(String email) {
        return new Student(null, "Test Student", email, "MCA", 1000.0);
    }

    @Test
    void savingFillsAuditFieldsAndVersion() {

        Student saved = repository.saveAndFlush(student("audit-test@example.com"));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(0L, saved.getVersion().longValue());
    }

    @Test
    void updatingIncrementsVersion() {

        Student saved = repository.saveAndFlush(student("version-test@example.com"));
        long before = saved.getVersion().longValue();

        saved.setName("Changed Name");
        Student updated = repository.saveAndFlush(saved);

        assertEquals(before + 1, updated.getVersion().longValue());
    }

    @Test
    void databaseRejectsDuplicateEmail() {

        repository.saveAndFlush(student("dup-test@example.com"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(student("dup-test@example.com")));
    }

    @Test
    void existsByEmailQueriesWork() {

        Student saved = repository.saveAndFlush(student("exists-test@example.com"));

        assertTrue(repository.existsByEmail("exists-test@example.com"));
        assertFalse(repository.existsByEmail("nobody-has-this@example.com"));

        // same student -> not a duplicate of itself
        assertFalse(repository.existsByEmailAndIdNot(
                "exists-test@example.com", saved.getId()));

        // a different id -> the e-mail counts as taken
        assertTrue(repository.existsByEmailAndIdNot(
                "exists-test@example.com", saved.getId() + 1));
    }
}
