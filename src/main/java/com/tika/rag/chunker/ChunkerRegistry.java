package com.tika.rag.chunker;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChunkerRegistry {

    private final Map<String, ChunkingStrategy> strategies;

    public ChunkerRegistry(List<ChunkingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(ChunkingStrategy::getName, Function.identity()));
    }

    public ChunkingStrategy getStrategy(String name) {
        ChunkingStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unknown chunking strategy: '" + name + "'. Available: " + strategies.keySet());
        }
        return strategy;
    }

    public List<String> availableStrategies() {
        return List.copyOf(strategies.keySet());
    }
}
