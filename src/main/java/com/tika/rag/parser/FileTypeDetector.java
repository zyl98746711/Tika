package com.tika.rag.parser;

import com.tika.rag.exception.UnsupportedFormatException;
import com.tika.rag.model.SupportedFormat;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

@Component
public class FileTypeDetector {

    private static final List<SupportedFormat> SUPPORTED_FORMATS = List.of(
            new SupportedFormat("application/pdf", "PDF", List.of("pdf")),
            new SupportedFormat("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "Microsoft Word (DOCX)", List.of("docx")),
            new SupportedFormat("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "Microsoft PowerPoint (PPTX)", List.of("pptx")),
            new SupportedFormat("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "Microsoft Excel (XLSX)", List.of("xlsx")),
            new SupportedFormat("application/msword",
                    "Microsoft Word (DOC)", List.of("doc")),
            new SupportedFormat("application/vnd.ms-powerpoint",
                    "Microsoft PowerPoint (PPT)", List.of("ppt")),
            new SupportedFormat("application/vnd.ms-excel",
                    "Microsoft Excel (XLS)", List.of("xls")),
            new SupportedFormat("text/plain", "Plain Text", List.of("txt")),
            new SupportedFormat("text/markdown", "Markdown", List.of("md", "markdown")),
            new SupportedFormat("text/html", "HTML", List.of("html", "htm")),
            new SupportedFormat("application/epub+zip", "EPUB", List.of("epub")),
            new SupportedFormat("application/rtf", "Rich Text Format", List.of("rtf")),
            new SupportedFormat("application/vnd.oasis.opendocument.text",
                    "OpenDocument Text", List.of("odt")),
            new SupportedFormat("application/vnd.oasis.opendocument.spreadsheet",
                    "OpenDocument Spreadsheet", List.of("ods")),
            new SupportedFormat("application/vnd.oasis.opendocument.presentation",
                    "OpenDocument Presentation", List.of("odp"))
    );

    private final Tika tika;
    private final MimeTypes mimeTypes;

    public FileTypeDetector() {
        this.tika = new Tika();
        this.mimeTypes = MimeTypes.getDefaultMimeTypes();
    }

    public String detect(InputStream inputStream, String fileName) {
        try {
            BufferedInputStream buffered = inputStream instanceof BufferedInputStream
                    ? (BufferedInputStream) inputStream
                    : new BufferedInputStream(inputStream);
            String detected = tika.detect(buffered, fileName);
            return detected != null ? detected : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    public SupportedFormat resolve(String mimeType) {
        return SUPPORTED_FORMATS.stream()
                .filter(f -> f.mimeType().equals(mimeType))
                .findFirst()
                .orElseGet(() -> {
                    try {
                        MimeType type = mimeTypes.forName(mimeType);
                        String name = type.getName() != null ? type.getName() : mimeType;
                        List<String> exts = List.of(type.getExtension().replaceFirst("^\\.", ""));
                        return new SupportedFormat(mimeType, name, exts);
                    } catch (MimeTypeException e) {
                        return new SupportedFormat(mimeType, mimeType, List.of());
                    }
                });
    }

    public void validate(String mimeType) {
        boolean supported = SUPPORTED_FORMATS.stream()
                .anyMatch(f -> f.mimeType().equals(mimeType))
                || mimeType.startsWith("text/")
                || mimeType.startsWith("image/");
        if (!supported) {
            throw UnsupportedFormatException.forMimeType(mimeType);
        }
    }

    public List<SupportedFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }
}
