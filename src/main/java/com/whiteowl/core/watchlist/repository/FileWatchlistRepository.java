package com.whiteowl.core.watchlist.repository;

import com.whiteowl.core.watchlist.model.Watchlist;
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
public final class FileWatchlistRepository implements WatchlistRepository {

    private static final String DIR_NAME = "watchlists";
    private static final String FILE_EXTENSION = ".csv";

    private final Path dirPath;

    public FileWatchlistRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", DIR_NAME));
    }

    public FileWatchlistRepository(Path dirPath) {
        this.dirPath = dirPath;
        log.info("FileWatchlistRepository initialized with dir={}", dirPath);
    }

    @Override
    public List<Watchlist> loadAll() {
        if (!Files.exists(dirPath)) return new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            List<Watchlist> result = files
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .map(this::loadWatchlist)
                    .filter(w -> w != null)
                    .collect(Collectors.toCollection(ArrayList::new));
            log.debug("Loaded {} watchlists", result.size());
            return result;
        } catch (IOException e) {
            log.error("Failed to load watchlists", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<Watchlist> watchlists) {
        try {
            ensureDirExists();
            Set<String> activeFiles = watchlists.stream()
                    .map(w -> toFileName(w.getName()))
                    .collect(Collectors.toSet());
            deleteOrphanFiles(activeFiles);
            for (Watchlist w : watchlists) {
                saveWatchlist(w);
            }
            log.debug("Saved {} watchlists", watchlists.size());
        } catch (IOException e) {
            log.error("Failed to save watchlists", e);
        }
    }

    private Watchlist loadWatchlist(Path file) {
        try {
            String name = toWatchlistName(file.getFileName().toString());
            List<String> lines = Files.readAllLines(file);
            List<String> scripIds = lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .toList();
            return new Watchlist(name, scripIds);
        } catch (Exception e) {
            log.error("Failed to load watchlist from {}", file, e);
            return null;
        }
    }

    private void saveWatchlist(Watchlist w) throws IOException {
        Path file = dirPath.resolve(toFileName(w.getName()));
        Files.write(file, w.getScripIds());
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
            log.debug("Deleted orphan watchlist file {}", file.getFileName());
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

    static String toWatchlistName(String fileName) {
        return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
    }

}
