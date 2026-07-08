package com.whiteowl.core.note.repository;

import com.whiteowl.core.note.model.Note;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class FileNoteRepository implements NoteRepository {

    private static final String DIR_NAME = "notes";
    private static final String FILE_EXTENSION = ".txt";

    private final Path dirPath;

    public FileNoteRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", DIR_NAME));
    }

    public FileNoteRepository(Path dirPath) {
        this.dirPath = dirPath;
        log.info("FileNoteRepository initialized with dir={}", dirPath);
    }

    @Override
    public List<Note> loadAll() {
        if (!Files.exists(dirPath)) return new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            List<Note> result = files
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .map(this::loadNote)
                    .filter(n -> n != null)
                    .collect(Collectors.toCollection(ArrayList::new));
            log.debug("Loaded {} notes", result.size());
            return result;
        } catch (IOException e) {
            log.error("Failed to load notes", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void save(Note note) {
        try {
            ensureDirExists();
            Path file = dirPath.resolve(toFileName(note.getName()));
            Files.writeString(file, note.getContent());
            log.debug("Saved note '{}'", note.getName());
        } catch (IOException e) {
            log.error("Failed to save note '{}'", note.getName(), e);
        }
    }

    @Override
    public void delete(String name) {
        try {
            Path file = dirPath.resolve(toFileName(name));
            Files.deleteIfExists(file);
            log.debug("Deleted note '{}'", name);
        } catch (IOException e) {
            log.error("Failed to delete note '{}'", name, e);
        }
    }

    @Override
    public void rename(String oldName, String newName) {
        try {
            Path oldFile = dirPath.resolve(toFileName(oldName));
            Path newFile = dirPath.resolve(toFileName(newName));
            if (Files.exists(oldFile)) {
                Files.move(oldFile, newFile);
                log.debug("Renamed note '{}' to '{}'", oldName, newName);
            }
        } catch (IOException e) {
            log.error("Failed to rename note '{}' to '{}'", oldName, newName, e);
        }
    }

    private Note loadNote(Path file) {
        try {
            String name = toNoteName(file.getFileName().toString());
            String content = Files.readString(file);
            return new Note(name, content);
        } catch (Exception e) {
            log.error("Failed to load note from {}", file, e);
            return null;
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

    static String toNoteName(String fileName) {
        return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
    }

}
