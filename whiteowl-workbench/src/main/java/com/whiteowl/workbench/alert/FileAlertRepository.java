package com.whiteowl.workbench.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
public final class FileAlertRepository implements AlertRepository {

    private static final String DIR_NAME = "alerts";
    private static final String FILE_EXTENSION = ".json";

    private final Path dirPath;
    private final ObjectMapper objectMapper;

    public FileAlertRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", DIR_NAME));
    }

    public FileAlertRepository(Path dirPath) {
        this.dirPath = dirPath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        log.info("FileAlertRepository initialized with dir={}", dirPath);
    }

    @Override
    public List<AlertDefinition> loadAll() {
        if (!Files.exists(dirPath)) return new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            List<AlertDefinition> result = files
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .map(this::loadAlertDefinition)
                    .filter(a -> a != null)
                    .collect(Collectors.toCollection(ArrayList::new));
            log.debug("Loaded {} alert definitions", result.size());
            return result;
        } catch (IOException e) {
            log.error("Failed to load alert definitions", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void save(AlertDefinition alertDefinition) {
        try {
            ensureDirExists();
            Path file = dirPath.resolve(toFileName(alertDefinition.getId()));
            objectMapper.writeValue(file.toFile(), alertDefinition);
            log.debug("Saved alert definition {}", alertDefinition.getId());
        } catch (IOException e) {
            log.error("Failed to save alert definition {}", alertDefinition.getId(), e);
        }
    }

    @Override
    public void delete(String alertDefinitionId) {
        try {
            Path file = dirPath.resolve(toFileName(alertDefinitionId));
            Files.deleteIfExists(file);
            log.debug("Deleted alert definition {}", alertDefinitionId);
        } catch (IOException e) {
            log.error("Failed to delete alert definition {}", alertDefinitionId, e);
        }
    }

    @Override
    public void saveAll(List<AlertDefinition> alertDefinitions) {
        try {
            ensureDirExists();
            Set<String> activeFiles = alertDefinitions.stream()
                    .map(a -> toFileName(a.getId()))
                    .collect(Collectors.toSet());
            deleteOrphanFiles(activeFiles);
            for (AlertDefinition alertDefinition : alertDefinitions) {
                save(alertDefinition);
            }
            log.debug("Saved {} alert definitions", alertDefinitions.size());
        } catch (IOException e) {
            log.error("Failed to save alert definitions", e);
        }
    }

    private AlertDefinition loadAlertDefinition(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), AlertDefinition.class);
        } catch (Exception e) {
            log.error("Failed to load alert definition from {}", file, e);
            return null;
        }
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
            log.debug("Deleted orphan alert file {}", file.getFileName());
        } catch (IOException e) {
            log.error("Failed to delete orphan file {}", file, e);
        }
    }

    private void ensureDirExists() throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    private static String toFileName(String id) {
        return id + FILE_EXTENSION;
    }
}

