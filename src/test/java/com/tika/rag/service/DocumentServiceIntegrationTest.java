package com.tika.rag.service;

import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.response.ChunkResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Test
    void parsePlainTextFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                "Hello world. This is a test document.".getBytes()
        );

        ParsedDocument result = documentService.parseDocument(file);

        assertNotNull(result);
        assertTrue(result.text().contains("Hello world"));
        assertEquals("text/plain", result.format());
    }

    @Test
    void parseHtmlFile() {
        String html = "<html><body><h1>Title</h1><p>Content here.</p></body></html>";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.html", "text/html", html.getBytes()
        );

        ParsedDocument result = documentService.parseDocument(file);

        assertNotNull(result);
        assertTrue(result.text().contains("Title"));
        assertTrue(result.text().contains("Content"));
    }

    @Test
    void parseAndChunkFixedSize() {
        String text = "word ".repeat(500);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", text.getBytes()
        );
        ChunkingRequest request = new ChunkingRequest("fixedSize", 200, 50, null, null, null);

        ChunkResponse response = documentService.parseAndChunk(file, request);

        assertNotNull(response);
        assertTrue(response.totalChunks() >= 2);
        assertEquals(response.totalChunks(), response.chunks().size());
        assertFalse(response.chunks().isEmpty());
    }

    @Test
    void parseAndChunkTokenBased() {
        String text = "token ".repeat(1000);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", text.getBytes()
        );
        ChunkingRequest request = new ChunkingRequest("tokenBased", null, null, 200, 30, null);

        ChunkResponse response = documentService.parseAndChunk(file, request);

        assertNotNull(response);
        assertTrue(response.totalChunks() >= 2);
    }

    @Test
    void parseAndChunkHierarchical() {
        String text = """
                # Title

                ## Section 1

                Content of section 1 with enough text.

                ## Section 2

                Content of section 2 with different text.
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.md", "text/markdown", text.getBytes()
        );
        ChunkingRequest request = new ChunkingRequest("hierarchical", 2000, null, null, null, 10);

        ChunkResponse response = documentService.parseAndChunk(file, request);

        assertNotNull(response);
        assertTrue(response.totalChunks() >= 1);
    }

    @Test
    void parseFromResource_txt() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test-documents/sample.txt");
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.txt", "text/plain", bytes
        );

        ParsedDocument result = documentService.parseDocument(file);

        assertNotNull(result);
        assertTrue(result.text().contains("Apache Tika"));
    }

    @Test
    void parseFromResource_html() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test-documents/sample.html");
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.html", "text/html", bytes
        );

        ParsedDocument result = documentService.parseDocument(file);

        assertNotNull(result);
        assertTrue(result.text().contains("RAG Document Parsing"));
    }

    @Test
    void getSupportedFormats() {
        List<SupportedFormat> formats = documentService.getSupportedFormats();

        assertNotNull(formats);
        assertFalse(formats.isEmpty());
        assertTrue(formats.stream().anyMatch(f -> f.mimeType().equals("application/pdf")));
        assertTrue(formats.stream().anyMatch(f -> f.mimeType().equals("text/plain")));
    }

    @Test
    void parseDefaultStrategyWhenNotSpecified() {
        String text = "content ".repeat(200);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", text.getBytes()
        );
        ChunkingRequest request = new ChunkingRequest(null, null, null, null, null, null);

        ChunkResponse response = documentService.parseAndChunk(file, request);

        assertNotNull(response);
        assertTrue(response.totalChunks() >= 1);
    }
}
