package com.tika.rag.service;

import com.tika.rag.chunker.ChunkingStrategy;
import com.tika.rag.chunker.ChunkerRegistry;
import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.response.ChunkResponse;
import com.tika.rag.parser.DocumentParser;
import com.tika.rag.parser.FileTypeDetector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentParser documentParser;
    private final FileTypeDetector fileTypeDetector;
    private final ChunkerRegistry chunkerRegistry;

    public DocumentServiceImpl(DocumentParser documentParser,
                               FileTypeDetector fileTypeDetector,
                               ChunkerRegistry chunkerRegistry) {
        this.documentParser = documentParser;
        this.fileTypeDetector = fileTypeDetector;
        this.chunkerRegistry = chunkerRegistry;
    }

    @Override
    public ParsedDocument parseDocument(MultipartFile file) {
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            return documentParser.parse(is, fileName, file.getSize());
        } catch (Exception e) {
            throw new com.tika.rag.exception.DocumentParseException(
                    "Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public ChunkResponse parseAndChunk(MultipartFile file, ChunkingRequest request) {
        ParsedDocument document = parseDocument(file);

        ChunkingStrategy strategy = chunkerRegistry.getStrategy(request.effectiveStrategy());
        List<DocumentChunk> chunks = strategy.chunk(document, request);

        return new ChunkResponse(
                document.metadata(),
                document.format(),
                chunks.size(),
                chunks
        );
    }

    @Override
    public List<SupportedFormat> getSupportedFormats() {
        return fileTypeDetector.getSupportedFormats();
    }
}
