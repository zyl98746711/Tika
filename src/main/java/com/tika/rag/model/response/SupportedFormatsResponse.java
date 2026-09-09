package com.tika.rag.model.response;

import com.tika.rag.model.SupportedFormat;

import java.util.List;

public record SupportedFormatsResponse(
        List<SupportedFormat> formats
) {
}
