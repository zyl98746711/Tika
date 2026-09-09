package com.tika.rag.model.response;

import com.tika.rag.model.DocumentMetadata;

public record ParseResponse(
        String text,
        DocumentMetadata metadata,
        String format,
        long originalSizeBytes
) {
}
