package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("tokenBased")
public class TokenBasedChunker implements ChunkingStrategy {

    @Override
    public String getName() {
        return "tokenBased";
    }

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document, ChunkingRequest config) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int tokenLimit = config.effectiveTokenLimit();
        int overlapTokens = Math.min(config.effectiveOverlapTokens(), tokenLimit / 2);

        List<String> allTokens = TokenEstimator.tokenize(text);
        if (allTokens.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int index = 0;
        int tokenPos = 0;

        while (tokenPos < allTokens.size()) {
            int endTokenPos = Math.min(tokenPos + tokenLimit, allTokens.size());

            List<String> chunkTokens = allTokens.subList(tokenPos, endTokenPos);
            String chunkText = String.join(" ", chunkTokens);

            int startOffset = findOffset(text, chunkTokens.getFirst(), tokenPos == 0 ? 0 : chunks.getLast().endOffset());
            int endOffset = startOffset + chunkText.length();
            if (endOffset > text.length()) endOffset = text.length();

            chunks.add(new DocumentChunk(
                    index++,
                    chunkText.trim(),
                    startOffset,
                    endOffset,
                    chunkTokens.size(),
                    Map.of("strategy", getName())
            ));

            if (endTokenPos >= allTokens.size()) break;
            tokenPos = endTokenPos - overlapTokens;
        }

        return chunks;
    }

    private int findOffset(String text, String token, int fromIndex) {
        int idx = text.indexOf(token, fromIndex);
        return idx >= 0 ? idx : fromIndex;
    }
}
