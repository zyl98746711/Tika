package com.tika.rag.model;

public record ParsedDocument(
        String text,
        DocumentMetadata metadata,
        String format,
        long originalSizeBytes
) {
}
