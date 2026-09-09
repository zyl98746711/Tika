package com.tika.rag.service;

import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.model.response.ChunkResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    ParsedDocument parseDocument(MultipartFile file);

    ChunkResponse parseAndChunk(MultipartFile file, ChunkingRequest request);

    List<SupportedFormat> getSupportedFormats();
}
