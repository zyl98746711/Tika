package com.tika.rag.controller;

import com.tika.rag.exception.GlobalExceptionHandler;
import com.tika.rag.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.DocumentMetadata;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.response.ChunkResponse;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void parseDocument() throws Exception {
        ParsedDocument doc = new ParsedDocument(
                "Extracted text content",
                new DocumentMetadata("Title", "Author", null, null, null, null,
                        "text/plain", null, null, Map.of()),
                "text/plain",
                100
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content".getBytes()
        );

        when(documentService.parseDocument(any())).thenReturn(doc);

        mockMvc.perform(multipart("/api/v1/documents/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Extracted text content"))
                .andExpect(jsonPath("$.format").value("text/plain"))
                .andExpect(jsonPath("$.metadata.title").value("Title"))
                .andExpect(jsonPath("$.metadata.author").value("Author"));
    }

    @Test
    void parseAndChunk() throws Exception {
        DocumentChunk chunk1 = new DocumentChunk(0, "chunk one", 0, 9, 2, Map.of("strategy", "fixedSize"));
        DocumentChunk chunk2 = new DocumentChunk(1, "chunk two", 10, 19, 2, Map.of("strategy", "fixedSize"));

        ChunkResponse response = new ChunkResponse(
                new DocumentMetadata(null, null, null, null, null, null, "text/plain", null, null, Map.of()),
                "text/plain",
                2,
                List.of(chunk1, chunk2)
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "chunk one chunk two".getBytes()
        );

        when(documentService.parseAndChunk(any(), any(ChunkingRequest.class))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/documents/chunk")
                        .file(file)
                        .param("strategy", "fixedSize")
                        .param("chunkSize", "100")
                        .param("overlap", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChunks").value(2))
                .andExpect(jsonPath("$.chunks[0].text").value("chunk one"))
                .andExpect(jsonPath("$.chunks[1].text").value("chunk two"));
    }

    @Test
    void getSupportedFormats() throws Exception {
        List<SupportedFormat> formats = List.of(
                new SupportedFormat("application/pdf", "PDF", List.of("pdf")),
                new SupportedFormat("text/plain", "Plain Text", List.of("txt"))
        );

        when(documentService.getSupportedFormats()).thenReturn(formats);

        mockMvc.perform(get("/api/v1/documents/formats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formats").isArray())
                .andExpect(jsonPath("$.formats[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.formats[0].name").value("PDF"))
                .andExpect(jsonPath("$.formats[1].mimeType").value("text/plain"));
    }

    @Test
    void parseDocumentMissingFile() throws Exception {
        mockMvc.perform(post("/api/v1/documents/parse"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parseAndChunkWithDefaultStrategy() throws Exception {
        ChunkResponse response = new ChunkResponse(
                new DocumentMetadata(null, null, null, null, null, null, "text/plain", null, null, Map.of()),
                "text/plain",
                1,
                List.of(new DocumentChunk(0, "text", 0, 4, 1, Map.of("strategy", "fixedSize")))
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "text".getBytes()
        );

        when(documentService.parseAndChunk(any(), any(ChunkingRequest.class))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/documents/chunk").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChunks").value(1));
    }
}
