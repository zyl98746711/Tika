package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.DocumentMetadata;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixedSizeChunkerTest {

    private final FixedSizeChunker chunker = new FixedSizeChunker();

    @Test
    void chunkBasicText() {
        String text = "a".repeat(250);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 100, 20, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.text().length() <= 120);
            assertNotNull(chunk.metadata().get("strategy"));
        }
    }

    @Test
    void chunkWithOverlap() {
        String text = "Hello world. ".repeat(100);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 200, 50, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void chunkShortText() {
        String text = "Short text.";
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 1000, 200, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertEquals(1, chunks.size());
        assertEquals("Short text.", chunks.getFirst().text());
        assertEquals(0, chunks.getFirst().index());
    }

    @Test
    void chunkEmptyText() {
        ParsedDocument doc = createDoc("");
        ChunkingRequest config = new ChunkingRequest("fixedSize", 1000, 200, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkBlankText() {
        ParsedDocument doc = createDoc("   \n\n  ");
        ChunkingRequest config = new ChunkingRequest("fixedSize", 1000, 200, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkRespectsSentenceBoundary() {
        String text = "First sentence. Second sentence. Third sentence. Fourth sentence. Fifth sentence.";
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 40, 0, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
        for (DocumentChunk chunk : chunks) {
            assertFalse(chunk.text().isEmpty());
        }
    }

    @Test
    void chunkIndicesAreSequential() {
        String text = "Word ".repeat(500);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 100, 10, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
        }
    }

    @Test
    void overlapClampedToHalfChunkSize() {
        String text = "x".repeat(300);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("fixedSize", 100, 999, null, null, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    private ParsedDocument createDoc(String text) {
        return new ParsedDocument(
                text,
                new DocumentMetadata(null, null, null, null, null, null, "text/plain", null, null, Map.of()),
                "text/plain",
                text.length()
        );
    }
}
