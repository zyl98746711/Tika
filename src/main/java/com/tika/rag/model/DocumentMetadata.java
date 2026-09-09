package com.tika.rag.model;

import java.time.LocalDateTime;
import java.util.Map;

public record DocumentMetadata(
        String title,
        String author,
        String subject,
        String keywords,
        LocalDateTime creationDate,
        LocalDateTime modifiedDate,
        String contentType,
        Integer pageCount,
        Integer wordCount,
        Map<String, String> additionalProperties
) {
}
