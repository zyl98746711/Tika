# Sample Document for RAG Testing

## Introduction

This is a sample Markdown document used for testing the RAG document parsing service.
It contains multiple sections with varying content to test different chunking strategies.

## Background

Apache Tika is a content analysis toolkit that can detect and extract metadata and text
from thousands of file types. It serves as the core engine for our document parsing service.

### Key Features

- Unified API for multiple document formats
- Metadata extraction (author, title, dates)
- Text content extraction with structure preservation
- MIME type detection

### Architecture

The system follows a layered architecture:
1. Controller layer handles HTTP requests
2. Service layer orchestrates business logic
3. Parser layer wraps Apache Tika
4. Chunker layer provides multiple text segmentation strategies

## Implementation Details

### Document Parsing

The parsing process involves several steps:
1. File type detection using Tika's detector
2. Content extraction using format-specific parsers
3. Metadata extraction from document properties
4. Text normalization and cleanup

### Text Chunking

We support three chunking strategies:

**Fixed Size**: Splits text into chunks of approximately equal character count with configurable overlap.
Best for uniform processing when document structure is not important.

**Token Based**: Splits text based on approximate token count, which correlates better with
LLM context windows. Uses whitespace-based tokenization with long-word subdivision.

**Hierarchical**: Respects document structure by splitting on headings and paragraphs.
Merges small sections and splits large ones to maintain coherent chunks.

## Conclusion

This document provides sufficient content and structure to test all parsing and chunking
features of the RAG document processing service.
