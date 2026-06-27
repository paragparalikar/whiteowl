package com.whiteowl.core.examplegroup.repository;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.examplegroup.model.Example;
import com.whiteowl.core.examplegroup.model.ExampleGroup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class FileExampleGroupRepository implements ExampleGroupRepository {

    private static final String DIR_NAME = "example-groups";
    private static final String FILE_EXTENSION = ".csv";
    private static final String CSV_SEPARATOR = ",";
    private static final int EXPECTED_FIELD_COUNT = 3;

    private final Path dirPath;

    public FileExampleGroupRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", DIR_NAME));
    }

    public FileExampleGroupRepository(Path dirPath) {
        this.dirPath = dirPath;
        log.info("FileExampleGroupRepository initialized with dir={}", dirPath);
    }

    @Override
    public List<ExampleGroup> loadAll() {
        if (!Files.exists(dirPath)) return new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            List<ExampleGroup> result = files
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .map(this::loadExampleGroup)
                    .filter(g -> g != null)
                    .collect(Collectors.toCollection(ArrayList::new));
            log.debug("Loaded {} example groups", result.size());
            return result;
        } catch (IOException e) {
            log.error("Failed to load example groups", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<ExampleGroup> groups) {
        try {
            ensureDirExists();
            Set<String> activeFiles = groups.stream()
                    .map(g -> toFileName(g.getName()))
                    .collect(Collectors.toSet());
            deleteOrphanFiles(activeFiles);
            for (ExampleGroup g : groups) {
                saveExampleGroup(g);
            }
            log.debug("Saved {} example groups", groups.size());
        } catch (IOException e) {
            log.error("Failed to save example groups", e);
        }
    }

    private ExampleGroup loadExampleGroup(Path file) {
        try {
            String name = toGroupName(file.getFileName().toString());
            List<String> lines = Files.readAllLines(file);
            List<Example> examples = lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(this::parseExample)
                    .filter(e -> e != null)
                    .toList();
            return new ExampleGroup(name, examples);
        } catch (Exception e) {
            log.error("Failed to load example group from {}", file, e);
            return null;
        }
    }

    private Example parseExample(String line) {
        try {
            String[] parts = line.split(CSV_SEPARATOR, EXPECTED_FIELD_COUNT);
            if (parts.length < EXPECTED_FIELD_COUNT) return null;
            String scripId = parts[0].trim();
            Timeframe timeframe = Timeframe.valueOf(parts[1].trim());
            long timestamp = Long.parseLong(parts[2].trim());
            return new Example(scripId, timeframe, timestamp);
        } catch (Exception e) {
            log.warn("Skipping malformed example line: {}", line, e);
            return null;
        }
    }

    private void saveExampleGroup(ExampleGroup g) throws IOException {
        Path file = dirPath.resolve(toFileName(g.getName()));
        List<String> lines = g.getExamples().stream()
                .map(e -> e.getScripId() + CSV_SEPARATOR + e.getTimeframe().name() + CSV_SEPARATOR + e.getTimestamp())
                .toList();
        Files.write(file, lines);
    }

    private void deleteOrphanFiles(Set<String> activeFiles) throws IOException {
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .filter(p -> !activeFiles.contains(p.getFileName().toString()))
                    .forEach(this::deleteQuietly);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
            log.debug("Deleted orphan example group file {}", file.getFileName());
        } catch (IOException e) {
            log.error("Failed to delete orphan file {}", file, e);
        }
    }

    private void ensureDirExists() throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    static String toFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_") + FILE_EXTENSION;
    }

    static String toGroupName(String fileName) {
        return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
    }

}
