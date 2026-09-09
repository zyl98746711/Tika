package com.tika.rag.controller;

import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.response.ChunkResponse;
import com.tika.rag.model.response.ParseResponse;
import com.tika.rag.model.response.SupportedFormatsResponse;
import com.tika.rag.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParseResponse> parseDocument(
            @RequestParam("file") MultipartFile file) {

        ParsedDocument document = documentService.parseDocument(file);

        ParseResponse response = new ParseResponse(
                document.text(),
                document.metadata(),
                document.format(),
                document.originalSizeBytes()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkResponse> parseAndChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "strategy", defaultValue = "fixedSize") String strategy,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            @RequestParam(value = "overlap", required = false) Integer overlap,
            @RequestParam(value = "tokenLimit", required = false) Integer tokenLimit,
            @RequestParam(value = "overlapTokens", required = false) Integer overlapTokens,
            @RequestParam(value = "minChunkSize", required = false) Integer minChunkSize) {

        ChunkingRequest request = new ChunkingRequest(
                strategy, chunkSize, overlap, tokenLimit, overlapTokens, minChunkSize
        );

        ChunkResponse response = documentService.parseAndChunk(file, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/formats")
    public ResponseEntity<SupportedFormatsResponse> getSupportedFormats() {
        List<SupportedFormat> formats = documentService.getSupportedFormats();
        return ResponseEntity.ok(new SupportedFormatsResponse(formats));
    }
}
