package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.DocumentMetadata;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.util.TokenEstimator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenBasedChunkerTest {

    private final TokenBasedChunker chunker = new TokenBasedChunker();

    @Test
    void chunkByTokenCount() {
        String text = "word ".repeat(1000);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("tokenBased", null, null, 200, 50, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 3);
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.approximateTokenCount() <= 200);
        }
    }

    @Test
    void chunkWithTokenOverlap() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("token").append(i).append(" ");
        }
        String text = sb.toString();
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("tokenBased", null, null, 100, 20, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void chunkShortText() {
        String text = "just a few words";
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("tokenBased", null, null, 512, 50, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().text().contains("few words"));
    }

    @Test
    void chunkEmptyText() {
        ParsedDocument doc = createDoc("");
        ChunkingRequest config = new ChunkingRequest("tokenBased", null, null, 512, 50, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void tokenCountApproximation() {
        String text = "The quick brown fox jumps over the lazy dog";
        int count = TokenEstimator.estimateTokenCount(text);

        assertTrue(count >= 8 && count <= 12);
    }

    @Test
    void tokenEstimatorHandlesLongWords() {
        String text = "short supercalifragilisticexpialidocious word";
        int count = TokenEstimator.estimateTokenCount(text);

        assertTrue(count >= 3);
    }

    @Test
    void tokenEstimatorEmpty() {
        assertEquals(0, TokenEstimator.estimateTokenCount(""));
        assertEquals(0, TokenEstimator.estimateTokenCount(null));
        assertEquals(0, TokenEstimator.estimateTokenCount("   "));
    }

    @Test
    void chunkIndicesAreSequential() {
        String text = "alpha beta gamma delta ".repeat(200);
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("tokenBased", null, null, 50, 10, null);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
        }
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
