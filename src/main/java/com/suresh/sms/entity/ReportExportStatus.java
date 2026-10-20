package com.suresh.sms.entity;

/**
 * Lifecycle of a {@link ReportExport}. Set to {@code PENDING} the moment the
 * API accepts the request, before the batch job has even been handed to the
 * (async) {@code JobLauncher} - see {@code ReportExportService}.
 */
public enum ReportExportStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
