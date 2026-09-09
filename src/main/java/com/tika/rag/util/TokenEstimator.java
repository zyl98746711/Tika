package com.tika.rag.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class TokenEstimator {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\S+");
    private static final int LONG_WORD_THRESHOLD = 12;

    private TokenEstimator() {
    }

    public static int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return tokenize(text).size();
    }

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (word.length() <= LONG_WORD_THRESHOLD) {
                tokens.add(word);
            } else {
                String[] parts = splitLongWord(word);
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        tokens.add(part);
                    }
                }
            }
        }

        return tokens;
    }

    private static String[] splitLongWord(String word) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = start; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c == '.' || c == ',' || c == ';' || c == ':' || c == '-' || c == '_'
                    || c == '/' || c == '\\' || c == '(' || c == ')') {
                if (i > start) {
                    parts.add(word.substring(start, i));
                }
                start = i + 1;
            } else if (i - start >= 8 && Character.isUpperCase(c) && i > 0 && Character.isLowerCase(word.charAt(i - 1))) {
                parts.add(word.substring(start, i));
                start = i;
            }
        }
        if (start < word.length()) {
            String remaining = word.substring(start);
            if (remaining.length() > LONG_WORD_THRESHOLD) {
                for (int i = 0; i < remaining.length(); i += 8) {
                    parts.add(remaining.substring(i, Math.min(i + 8, remaining.length())));
                }
            } else {
                parts.add(remaining);
            }
        }
        return parts.toArray(new String[0]);
    }
}
