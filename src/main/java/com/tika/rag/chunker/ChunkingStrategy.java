package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;

import java.util.List;

public interface ChunkingStrategy {

    String getName();

    List<DocumentChunk> chunk(ParsedDocument document, ChunkingRequest config);
}
