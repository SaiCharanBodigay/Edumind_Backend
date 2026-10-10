package com.edumind.backend.dto;

import java.time.Instant;

public class LearningMaterialResponse {

    private final Long id;
    private final Long courseId;
    private final String filename;
    private final String contentType;
    private final Long size;
    private final Instant uploadedAt;

    public LearningMaterialResponse(
            Long id,
            Long courseId,
            String filename,
            String contentType,
            Long size,
            Instant uploadedAt) {
        this.id = id;
        this.courseId = courseId;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSize() {
        return size;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
