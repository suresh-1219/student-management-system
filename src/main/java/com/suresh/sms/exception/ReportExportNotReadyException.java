package com.suresh.sms.exception;

/**
 * Thrown when a download is requested for an export that hasn't finished
 * (or has failed) - the request itself is well-formed and the export exists,
 * it just isn't in a downloadable state yet. Mapped to 409 (Conflict), not
 * 404: the resource ({@code ReportExport}) exists, its current state just
 * conflicts with "give me the file".
 */
public class ReportExportNotReadyException extends RuntimeException {

    public ReportExportNotReadyException(String message) {
        super(message);
    }
}
