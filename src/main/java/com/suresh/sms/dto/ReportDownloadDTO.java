package com.suresh.sms.dto;

/**
 * A presigned download link for a completed report export - the {@code
 * ReportExport} equivalent of {@link SyllabusDTO}. Only returned once the
 * export's status is {@code COMPLETED}; see {@code ReportExportService
 * #getExportDownload}.
 */
public class ReportDownloadDTO {

    private String filename;
    private long sizeBytes;
    private String downloadUrl;
    private int expiresInSeconds;

    public ReportDownloadDTO() {
    }

    public ReportDownloadDTO(String filename, long sizeBytes, String downloadUrl, int expiresInSeconds) {
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.downloadUrl = downloadUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
