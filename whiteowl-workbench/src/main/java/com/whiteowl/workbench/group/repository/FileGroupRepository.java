package com.whiteowl.workbench.group.repository;

import com.whiteowl.workbench.group.model.Group;
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
public final class FileGroupRepository implements GroupRepository {

    private static final String DIR_NAME = "groups";
    private static final String FILE_EXTENSION = ".csv";

    private final Path dirPath;

    public FileGroupRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", DIR_NAME));
    }

    public FileGroupRepository(Path dirPath) {
        this.dirPath = dirPath;
        log.info("FileGroupRepository initialized with dir={}", dirPath);
    }

    @Override
    public List<Group> loadAll() {
        if (!Files.exists(dirPath)) return new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            List<Group> result = files
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .map(this::loadGroup)
                    .filter(g -> g != null)
                    .collect(Collectors.toCollection(ArrayList::new));
            log.debug("Loaded {} groups", result.size());
            return result;
        } catch (IOException e) {
            log.error("Failed to load groups", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<Group> groups) {
        try {
            ensureDirExists();
            Set<String> activeFiles = groups.stream()
                    .map(g -> toFileName(g.getName()))
                    .collect(Collectors.toSet());
            deleteOrphanFiles(activeFiles);
            for (Group g : groups) {
                saveGroup(g);
            }
            log.debug("Saved {} groups", groups.size());
        } catch (IOException e) {
            log.error("Failed to save groups", e);
        }
    }

    private Group loadGroup(Path file) {
        try {
            String name = toGroupName(file.getFileName().toString());
            List<String> lines = Files.readAllLines(file);
            List<String> scripIds = lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .toList();
            return new Group(name, scripIds);
        } catch (Exception e) {
            log.error("Failed to load group from {}", file, e);
            return null;
        }
    }

    private void saveGroup(Group g) throws IOException {
        Path file = dirPath.resolve(toFileName(g.getName()));
        Files.write(file, g.getScripIds());
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
            log.debug("Deleted orphan group file {}", file.getFileName());
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
