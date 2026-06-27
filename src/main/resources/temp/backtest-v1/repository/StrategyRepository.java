package com.whiteowl.core.backtest.repository;

import com.whiteowl.core.backtest.model.Strategy;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
public final class StrategyRepository {

    private static final String STRATEGIES_DIR = "strategies";
    private static final String GROOVY_EXTENSION = ".groovy";
    private static final String DATA_DIR = "data";
    private static final String DEFAULT_TEMPLATE = """
            def fastPeriod = input("Fast Period", 20)
            def slowPeriod = input("Slow Period", 50)
            def fast = sma(close, fastPeriod)
            def slow = sma(close, slowPeriod)
            def cross = crossover(fast, slow)
            def crossDn = crossunder(fast, slow)
            
            for (int i = slowPeriod; i < barCount; i++) {
                if (cross[i]) longEntry(i)
                if (crossDn[i]) longExit(i)
            }
            """;

    private final Path strategiesDir;

    public StrategyRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), DATA_DIR, STRATEGIES_DIR));
    }

    public StrategyRepository(Path strategiesDir) {
        this.strategiesDir = strategiesDir;
        createDirectoryIfNeeded();
    }

    public List<Strategy> findAll() {
        if (!Files.exists(strategiesDir)) return Collections.emptyList();
        try (Stream<Path> files = Files.list(strategiesDir)) {
            return files
                    .filter(p -> p.toString().endsWith(GROOVY_EXTENSION))
                    .map(this::toStrategy)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list strategies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Strategy create(String name) throws IOException {
        String id = UUID.randomUUID().toString();
        Path scriptPath = strategiesDir.resolve(sanitizeFileName(name) + GROOVY_EXTENSION);
        Files.writeString(scriptPath, DEFAULT_TEMPLATE);
        return Strategy.builder()
                .id(id)
                .name(name)
                .scriptPath(scriptPath)
                .build();
    }

    public Strategy rename(Strategy strategy, String newName) throws IOException {
        Path newPath = strategiesDir.resolve(sanitizeFileName(newName) + GROOVY_EXTENSION);
        Files.move(strategy.getScriptPath(), newPath);
        return Strategy.builder()
                .id(strategy.getId())
                .name(newName)
                .scriptPath(newPath)
                .build();
    }

    public void delete(Strategy strategy) throws IOException {
        Files.deleteIfExists(strategy.getScriptPath());
    }

    public String loadScript(Strategy strategy) throws IOException {
        return Files.readString(strategy.getScriptPath());
    }

    public void saveScript(Strategy strategy, String content) throws IOException {
        Files.writeString(strategy.getScriptPath(), content);
    }

    private Strategy toStrategy(Path path) {
        String fileName = path.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - GROOVY_EXTENSION.length());
        return Strategy.builder()
                .id(name)
                .name(name)
                .scriptPath(path)
                .build();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void createDirectoryIfNeeded() {
        try {
            Files.createDirectories(strategiesDir);
        } catch (IOException e) {
            log.error("Failed to create strategies directory: {}", e.getMessage());
        }
    }

}
