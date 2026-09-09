package com.tika.rag.model;

import java.util.List;

public record SupportedFormat(
        String mimeType,
        String name,
        List<String> extensions
) {
}
