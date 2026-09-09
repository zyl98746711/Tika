package com.tika.rag.exception;

public class UnsupportedFormatException extends RuntimeException {

    public UnsupportedFormatException(String message) {
        super(message);
    }

    public static UnsupportedFormatException forMimeType(String mimeType) {
        return new UnsupportedFormatException("Unsupported document format: " + mimeType);
    }
}
