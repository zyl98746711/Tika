package com.tika.rag.model;

import java.util.Map;

public record DocumentChunk(
        int index,
        String text,
        int startOffset,
        int endOffset,
        int approximateTokenCount,
        Map<String, Object> metadata
) {
}
