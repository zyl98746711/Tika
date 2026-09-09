package com.tika.rag.model.request;

public record ChunkingRequest(
        String strategy,
        Integer chunkSize,
        Integer overlap,
        Integer tokenLimit,
        Integer overlapTokens,
        Integer minChunkSize
) {
    public static final String DEFAULT_STRATEGY = "fixedSize";
    public static final int DEFAULT_CHUNK_SIZE = 1000;
    public static final int DEFAULT_OVERLAP = 200;
    public static final int DEFAULT_TOKEN_LIMIT = 512;
    public static final int DEFAULT_OVERLAP_TOKENS = 50;
    public static final int DEFAULT_MIN_CHUNK_SIZE = 100;

    public String effectiveStrategy() {
        return strategy != null && !strategy.isBlank() ? strategy : DEFAULT_STRATEGY;
    }

    public int effectiveChunkSize() {
        return chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
    }

    public int effectiveOverlap() {
        return overlap != null && overlap >= 0 ? overlap : DEFAULT_OVERLAP;
    }

    public int effectiveTokenLimit() {
        return tokenLimit != null && tokenLimit > 0 ? tokenLimit : DEFAULT_TOKEN_LIMIT;
    }

    public int effectiveOverlapTokens() {
        return overlapTokens != null && overlapTokens >= 0 ? overlapTokens : DEFAULT_OVERLAP_TOKENS;
    }

    public int effectiveMinChunkSize() {
        return minChunkSize != null && minChunkSize > 0 ? minChunkSize : DEFAULT_MIN_CHUNK_SIZE;
    }
}
