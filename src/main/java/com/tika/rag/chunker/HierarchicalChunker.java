package com.tika.rag.chunker;

import com.tika.rag.model.DocumentChunk;
import com.tika.rag.model.ParsedDocument;
import com.tika.rag.model.request.ChunkingRequest;
import com.tika.rag.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("hierarchical")
public class HierarchicalChunker implements ChunkingStrategy {

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern HTML_HEADING_PATTERN = Pattern.compile(
            "<h([1-6])[^>]*>(.*?)</h[1-6]>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n{2,}");

    @Override
    public String getName() {
        return "hierarchical";
    }

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document, ChunkingRequest config) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int maxChunkSize = config.effectiveChunkSize();
        int minChunkSize = config.effectiveMinChunkSize();

        List<Section> sections = extractSections(text);
        List<Section> merged = mergeSmallSections(sections, minChunkSize);

        List<DocumentChunk> chunks = new ArrayList<>();
        int index = 0;

        for (Section section : merged) {
            String trimmed = section.content.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= maxChunkSize) {
                int tokenCount = TokenEstimator.estimateTokenCount(trimmed);
                Map<String, Object> meta = new HashMap<>();
                meta.put("strategy", getName());
                meta.put("heading", section.heading);
                meta.put("level", section.level);
                if (section.parentHeading != null) {
                    meta.put("parentHeading", section.parentHeading);
                }

                chunks.add(new DocumentChunk(
                        index++,
                        trimmed,
                        section.startOffset,
                        section.startOffset + section.content.length(),
                        tokenCount,
                        meta
                ));
            } else {
                List<DocumentChunk> subChunks = splitLargeSection(section, trimmed, maxChunkSize, index);
                chunks.addAll(subChunks);
                index += subChunks.size();
            }
        }

        return chunks;
    }

    private List<Section> extractSections(String text) {
        List<Section> sections = new ArrayList<>();

        List<HeadingMatch> headings = new ArrayList<>();

        Matcher mdMatcher = HEADING_PATTERN.matcher(text);
        while (mdMatcher.find()) {
            int level = mdMatcher.group(1).length();
            String title = mdMatcher.group(2).trim();
            headings.add(new HeadingMatch(mdMatcher.start(), mdMatcher.end(), level, title));
        }

        Matcher htmlMatcher = HTML_HEADING_PATTERN.matcher(text);
        while (htmlMatcher.find()) {
            int level = Integer.parseInt(htmlMatcher.group(1));
            String title = htmlMatcher.group(2).replaceAll("<[^>]+>", "").trim();
            headings.add(new HeadingMatch(htmlMatcher.start(), htmlMatcher.end(), level, title));
        }

        headings.sort((a, b) -> Integer.compare(a.start, b.start));

        if (headings.isEmpty()) {
            String[] paragraphs = PARAGRAPH_PATTERN.split(text);
            int offset = 0;
            for (String para : paragraphs) {
                if (!para.isBlank()) {
                    int start = text.indexOf(para, offset);
                    sections.add(new Section(null, 0, para.trim(), start, null));
                    offset = start + para.length();
                }
            }
            return sections;
        }

        if (headings.getFirst().start > 0) {
            String preamble = text.substring(0, headings.getFirst().start).trim();
            if (!preamble.isEmpty()) {
                sections.add(new Section(null, 0, preamble, 0, null));
            }
        }

        for (int i = 0; i < headings.size(); i++) {
            HeadingMatch h = headings.get(i);
            int contentStart = h.end;
            int contentEnd = (i + 1 < headings.size()) ? headings.get(i + 1).start : text.length();
            String content = text.substring(contentStart, contentEnd).trim();

            String parentHeading = findParentHeading(headings, i);

            sections.add(new Section(h.title, h.level, content, contentStart, parentHeading));
        }

        return sections;
    }

    private String findParentHeading(List<HeadingMatch> headings, int currentIndex) {
        int currentLevel = headings.get(currentIndex).level;
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (headings.get(i).level < currentLevel) {
                return headings.get(i).title;
            }
        }
        return null;
    }

    private List<Section> mergeSmallSections(List<Section> sections, int minSize) {
        if (sections.isEmpty()) return sections;

        List<Section> merged = new ArrayList<>();
        Section current = sections.getFirst();

        for (int i = 1; i < sections.size(); i++) {
            Section next = sections.get(i);
            if (current.content.length() < minSize
                    && (next.parentHeading == null || next.parentHeading.equals(current.heading))) {
                String combined = current.content + "\n\n" + next.content;
                current = new Section(
                        current.heading,
                        current.level,
                        combined,
                        current.startOffset,
                        current.parentHeading
                );
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    private List<DocumentChunk> splitLargeSection(Section section, String text, int maxSize, int startIndex) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int pos = 0;
        int index = startIndex;

        while (pos < text.length()) {
            int end = Math.min(pos + maxSize, text.length());

            if (end < text.length()) {
                int breakAt = text.lastIndexOf('\n', end);
                if (breakAt > pos + maxSize / 2) {
                    end = breakAt;
                }
            }

            String chunkText = text.substring(pos, end).trim();
            if (!chunkText.isEmpty()) {
                int tokenCount = TokenEstimator.estimateTokenCount(chunkText);
                Map<String, Object> meta = new HashMap<>();
                meta.put("strategy", getName());
                meta.put("heading", section.heading);
                meta.put("level", section.level);
                meta.put("part", chunks.size() + 1);
                if (section.parentHeading != null) {
                    meta.put("parentHeading", section.parentHeading);
                }

                chunks.add(new DocumentChunk(
                        index++,
                        chunkText,
                        section.startOffset + pos,
                        section.startOffset + end,
                        tokenCount,
                        meta
                ));
            }
            pos = end;
        }

        return chunks;
    }

    private record HeadingMatch(int start, int end, int level, String title) {}

    private record Section(String heading, int level, String content, int startOffset, String parentHeading) {}
}
