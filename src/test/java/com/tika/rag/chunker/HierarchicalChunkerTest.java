package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.DocumentMetadata;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HierarchicalChunkerTest {

    private final HierarchicalChunker chunker = new HierarchicalChunker();

    @Test
    void chunkByMarkdownHeadings() {
        String text = """
                # Title

                Introduction paragraph.

                ## Section One

                Content of section one with enough text to be meaningful.

                ## Section Two

                Content of section two with different information.

                ### Subsection

                Subsection content here.
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        boolean hasSectionOne = chunks.stream()
                .anyMatch(c -> c.metadata().containsKey("heading")
                        && "Section One".equals(c.metadata().get("heading")));
        assertTrue(hasSectionOne, "Should have a chunk for Section One");
    }

    @Test
    void chunkByHtmlHeadings() {
        String text = """
                <h1>Main Title</h1>
                <p>Introduction text.</p>
                <h2>Chapter 1</h2>
                <p>Chapter 1 content with sufficient detail.</p>
                <h2>Chapter 2</h2>
                <p>Chapter 2 content with different information.</p>
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void chunkWithoutHeadings() {
        String text = """
                First paragraph about topic A.

                Second paragraph about topic B.

                Third paragraph about topic C.
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void mergeSmallSections() {
        String text = """
                # Title

                ## A

                Short.

                ## B

                Also short.

                ## C

                This section has enough content to be meaningful and should not be merged with others.
                It contains multiple sentences to ensure it exceeds the minimum chunk size threshold.
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 50);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void splitLargeSections() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Large Section\n\n");
        for (int i = 0; i < 100; i++) {
            sb.append("Paragraph ").append(i).append(" with some content to make it longer.\n\n");
        }
        String text = sb.toString();
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 500, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.size() >= 2, "Large section should be split into multiple chunks");
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.text().length() <= 600);
        }
    }

    @Test
    void chunkEmptyText() {
        ParsedDocument doc = createDoc("");
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void headingMetadataIsPreserved() {
        String text = """
                # Document Title

                ## Chapter One

                Content of chapter one.

                ## Chapter Two

                Content of chapter two.
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        for (DocumentChunk chunk : chunks) {
            assertEquals("hierarchical", chunk.metadata().get("strategy"));
            if (chunk.metadata().containsKey("heading")) {
                assertNotNull(chunk.metadata().get("level"));
            }
        }
    }

    @Test
    void parentHeadingIsTracked() {
        String text = """
                # Book Title

                ## Chapter 1

                ### Section 1.1

                Content of section 1.1.
                """;
        ParsedDocument doc = createDoc(text);
        ChunkingRequest config = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        List<DocumentChunk> chunks = chunker.chunk(doc, config);

        boolean hasParentHeading = chunks.stream()
                .anyMatch(c -> "Chapter 1".equals(c.metadata().get("parentHeading")));
        assertTrue(hasParentHeading, "Subsection should have parent heading 'Chapter 1'");
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
