package com.tika.rag.parser;

import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;

import java.io.InputStream;
import java.util.List;

public interface DocumentParser {

    ParsedDocument parse(InputStream inputStream, String fileName, long fileSize);

    List<SupportedFormat> getSupportedFormats();

    boolean isSupported(String mimeType);
}
