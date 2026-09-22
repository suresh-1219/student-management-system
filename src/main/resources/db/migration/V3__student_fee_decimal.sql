-- V3: store fees as exact decimals instead of floating point (DOUBLE).
--
-- DOUBLE cannot represent values like 0.1 exactly, which is unacceptable for
-- money. DECIMAL(12,2) holds up to 10 digits before the decimal point and
-- exactly 2 after it. Existing values are converted (e.g. 50000 -> 50000.00).

ALTER TABLE students MODIFY COLUMN fee DECIMAL(12,2) NULL;
