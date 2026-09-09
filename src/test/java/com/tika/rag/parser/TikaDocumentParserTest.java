package com.tika.rag.parser;

import com.tika.rag.exception.DocumentParseException;
import com.tika.rag.exception.UnsupportedFormatException;
import com.tika.rag.model.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TikaDocumentParserTest {

    private TikaDocumentParser parser;

    @BeforeEach
    void setUp() {
        FileTypeDetector fileTypeDetector = new FileTypeDetector();
        parser = new TikaDocumentParser(fileTypeDetector);
    }

    @Test
    void parsePlainText() {
        String text = "Hello, this is a plain text document for testing.";
        InputStream is = new ByteArrayInputStream(text.getBytes());

        ParsedDocument result = parser.parse(is, "test.txt", text.length());

        assertNotNull(result);
        assertNotNull(result.text());
        assertTrue(result.text().contains("plain text document"));
        assertEquals("text/plain", result.format());
        assertNotNull(result.metadata());
    }

    @Test
    void parseHtml() {
        String html = """
                <html><head><title>Test</title></head>
                <body><h1>Title</h1><p>Paragraph content here.</p></body></html>
                """;
        InputStream is = new ByteArrayInputStream(html.getBytes());

        ParsedDocument result = parser.parse(is, "test.html", html.length());

        assertNotNull(result);
        assertTrue(result.text().contains("Title"));
        assertTrue(result.text().contains("Paragraph content"));
        assertEquals("text/html", result.format());
    }

    @Test
    void parseMarkdown() {
        String md = """
                # Heading 1
                Some content here.

                ## Heading 2
                More content here.
                """;
        InputStream is = new ByteArrayInputStream(md.getBytes());

        ParsedDocument result = parser.parse(is, "test.md", md.length());

        assertNotNull(result);
        assertTrue(result.text().contains("Heading 1"));
        assertTrue(result.text().contains("Heading 2"));
    }

    @Test
    void parseFromResource_txt() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test-documents/sample.txt");
        assertNotNull(is, "sample.txt should exist in test resources");

        ParsedDocument result = parser.parse(is, "sample.txt", 1024);

        assertNotNull(result);
        assertFalse(result.text().isBlank());
        assertTrue(result.text().contains("Apache Tika"));
        assertEquals("text/plain", result.format());
    }

    @Test
    void parseFromResource_html() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test-documents/sample.html");
        assertNotNull(is, "sample.html should exist in test resources");

        ParsedDocument result = parser.parse(is, "sample.html", 2048);

        assertNotNull(result);
        assertFalse(result.text().isBlank());
        assertTrue(result.text().contains("RAG Document Parsing"));
    }

    @Test
    void parseFromResource_md() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test-documents/sample.md");
        assertNotNull(is, "sample.md should exist in test resources");

        ParsedDocument result = parser.parse(is, "sample.md", 2048);

        assertNotNull(result);
        assertFalse(result.text().isBlank());
        assertTrue(result.text().contains("Introduction"));
    }

    @Test
    void parseEmptyContent() {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        ParsedDocument result = parser.parse(is, "empty.txt", 0);

        assertNotNull(result);
        assertTrue(result.text().isBlank() || result.text().isEmpty());
    }

    @Test
    void getSupportedFormats() {
        var formats = parser.getSupportedFormats();

        assertNotNull(formats);
        assertFalse(formats.isEmpty());
        assertTrue(formats.stream().anyMatch(f -> f.mimeType().equals("application/pdf")));
        assertTrue(formats.stream().anyMatch(f -> f.mimeType().equals("text/plain")));
        assertTrue(formats.stream().anyMatch(f -> f.mimeType().equals("text/html")));
    }

    @Test
    void isSupported() {
        assertTrue(parser.isSupported("application/pdf"));
        assertTrue(parser.isSupported("text/plain"));
        assertTrue(parser.isSupported("text/html"));
        assertFalse(parser.isSupported("application/unknown-format-xyz"));
    }
}
