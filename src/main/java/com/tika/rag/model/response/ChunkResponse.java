package com.tika.rag.model.response;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.DocumentMetadata;

import java.util.List;

public record ChunkResponse(
        DocumentMetadata metadata,
        String format,
        int totalChunks,
        List<DocumentChunk> chunks
) {
}
