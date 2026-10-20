package com.suresh.sms.dto;

/**
 * Response for {@code GET /courses/{id}/syllabus}: a short-lived presigned
 * URL the client downloads the file from directly, rather than this app
 * streaming the bytes itself.
 */
public class SyllabusDTO {

    private String filename;
    private long sizeBytes;
    private String downloadUrl;
    private int expiresInSeconds;

    public SyllabusDTO() {
    }

    public SyllabusDTO(
            String filename,
            long sizeBytes,
            String downloadUrl,
            int expiresInSeconds) {

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
