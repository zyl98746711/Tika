package com.tika.rag.parser;

import com.tika.rag.exception.DocumentParseException;
import com.tika.rag.exception.UnsupportedFormatException;
import com.tika.rag.model.DocumentMetadata;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.SupportedFormat;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TikaDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);

    private static final String RESOURCE_NAME = "resourceName";
    private static final String CONTENT_TYPE = "Content-Type";

    private final FileTypeDetector fileTypeDetector;

    public TikaDocumentParser(FileTypeDetector fileTypeDetector) {
        this.fileTypeDetector = fileTypeDetector;
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName, long fileSize) {
        try {
            byte[] bytes = inputStream.readAllBytes();

            // Handle empty files gracefully
            if (bytes.length == 0) {
                String mimeType = fileTypeDetector.detect(new ByteArrayInputStream(bytes), fileName);
                DocumentMetadata docMetadata = extractMetadata(new Metadata(), mimeType);
                return new ParsedDocument("", docMetadata, mimeType, fileSize);
            }

            String mimeType = fileTypeDetector.detect(new ByteArrayInputStream(bytes), fileName);
            fileTypeDetector.validate(mimeType);

            Parser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(RESOURCE_NAME, fileName);
            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);

            parser.parse(new ByteArrayInputStream(bytes), handler, metadata, context);

            String text = handler.toString();
            DocumentMetadata docMetadata = extractMetadata(metadata, mimeType);

            return new ParsedDocument(text, docMetadata, mimeType, fileSize);
        } catch (DocumentParseException | UnsupportedFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse document: {}", fileName, e);
            throw new DocumentParseException("Failed to parse document: " + fileName, e);
        }
    }

    @Override
    public List<SupportedFormat> getSupportedFormats() {
        return fileTypeDetector.getSupportedFormats();
    }

    @Override
    public boolean isSupported(String mimeType) {
        return fileTypeDetector.getSupportedFormats().stream()
                .anyMatch(f -> f.mimeType().equals(mimeType));
    }

    private DocumentMetadata extractMetadata(Metadata tikaMetadata, String contentType) {
        String title = tikaMetadata.get(TikaCoreProperties.TITLE);
        String author = tikaMetadata.get(TikaCoreProperties.CREATOR);
        String subject = tikaMetadata.get(TikaCoreProperties.DESCRIPTION);
        String keywords = tikaMetadata.get("meta:keyword");

        LocalDateTime creationDate = extractDate(tikaMetadata, TikaCoreProperties.CREATED);
        LocalDateTime modifiedDate = extractDate(tikaMetadata, TikaCoreProperties.MODIFIED);

        Integer pageCount = extractInt(tikaMetadata, "xmpTPg:nPages");
        if (pageCount == null) {
            pageCount = extractInt(tikaMetadata, "meta:page-count");
        }

        Integer wordCount = extractInt(tikaMetadata, "meta:word-count");

        Map<String, String> additional = new HashMap<>();
        for (String name : tikaMetadata.names()) {
            if (!isStandardProperty(name)) {
                String value = tikaMetadata.get(name);
                if (value != null && !value.isBlank()) {
                    additional.put(name, value);
                }
            }
        }

        return new DocumentMetadata(
                title, author, subject, keywords,
                creationDate, modifiedDate,
                contentType, pageCount, wordCount,
                additional
        );
    }

    private LocalDateTime extractDate(Metadata metadata, Property property) {
        try {
            Date date = metadata.getDate(property);
            if (date != null) {
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Integer extractInt(Metadata metadata, String propertyName) {
        try {
            String value = metadata.get(propertyName);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isStandardProperty(String name) {
        return name.equals(TikaCoreProperties.TITLE.getName())
                || name.equals(TikaCoreProperties.CREATOR.getName())
                || name.equals(TikaCoreProperties.DESCRIPTION.getName())
                || name.equals(TikaCoreProperties.CREATED.getName())
                || name.equals(TikaCoreProperties.MODIFIED.getName())
                || name.equals("meta:keyword")
                || name.equals("meta:page-count")
                || name.equals("meta:word-count")
                || name.equals("xmpTPg:nPages")
                || name.equals(RESOURCE_NAME)
                || name.equals(CONTENT_TYPE);
    }
}
