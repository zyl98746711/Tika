package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("fixedSize")
public class FixedSizeChunker implements ChunkingStrategy {

    @Override
    public String getName() {
        return "fixedSize";
    }

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document, ChunkingRequest config) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = config.effectiveChunkSize();
        int overlap = Math.min(config.effectiveOverlap(), chunkSize / 2);
        int step = chunkSize - overlap;

        List<DocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());

            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, pos, end);
                if (breakPoint > pos) {
                    end = breakPoint;
                }
            }

            String chunkText = text.substring(pos, end).trim();
            if (!chunkText.isEmpty()) {
                int tokenCount = TokenEstimator.estimateTokenCount(chunkText);
                chunks.add(new DocumentChunk(
                        index++,
                        chunkText,
                        pos,
                        end,
                        tokenCount,
                        Map.of("strategy", getName())
                ));
            }

            if (end >= text.length()) break;
            pos += step;
        }

        return chunks;
    }

    private int findBreakPoint(String text, int start, int preferredEnd) {
        int searchStart = Math.max(start + 1, preferredEnd - 100);

        for (int i = preferredEnd; i >= searchStart; i--) {
            char c = text.charAt(i - 1);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i;
            }
        }

        for (int i = preferredEnd; i >= searchStart; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }

        return preferredEnd;
    }
}
