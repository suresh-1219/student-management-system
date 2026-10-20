package com.suresh.sms.entity;

/**
 * What a {@link ReportExport} is a report of. Only one kind exists today
 * (all students, as CSV - see {@code StudentExportBatchConfig}), but this is
 * an enum rather than a hardcoded string so a later report (courses,
 * enrollments, ...) is a new constant plus a new batch job, not a schema
 * change.
 */
public enum ReportExportType {
    STUDENTS
}
