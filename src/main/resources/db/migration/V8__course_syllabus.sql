-- Adds an optional syllabus file attachment to Course. The file itself
-- lives in S3-compatible object storage (see StorageService), not the
-- database - these columns only record where it is and what it originally
-- was. All nullable: most courses will simply have no syllabus uploaded.
ALTER TABLE courses
    ADD COLUMN syllabus_object_key        VARCHAR(255) NULL,
    ADD COLUMN syllabus_original_filename VARCHAR(255) NULL,
    ADD COLUMN syllabus_content_type      VARCHAR(100) NULL,
    ADD COLUMN syllabus_size_bytes        BIGINT       NULL;
