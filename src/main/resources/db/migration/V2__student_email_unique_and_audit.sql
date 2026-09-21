-- V2: student e-mail must be unique, plus audit timestamps and a version
--     column for optimistic locking.
--
-- This is ONE statement on purpose: MySQL applies a single ALTER TABLE
-- all-or-nothing. If two students already share an e-mail, the whole
-- statement fails with "Duplicate entry ... for key 'uk_students_email'"
-- and the table is left untouched. Fix the duplicates, then start again.
--
-- Existing rows get created_at / updated_at = the time of this migration,
-- and version = 0.

ALTER TABLE students
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN version    BIGINT      NOT NULL DEFAULT 0,
    ADD CONSTRAINT uk_students_email UNIQUE (email);
