package com.whiteowl.workbench.charting.drawing.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Slf4j
public final class FileDrawingRepository implements DrawingRepository {

    private static final String FILE_NAME = "drawings.json";
    private static final TypeReference<List<DrawingDto>> LIST_TYPE = new TypeReference<>() {};

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    public FileDrawingRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", "drawings"));
    }

    public FileDrawingRepository(Path baseDir) {
        this.baseDir = baseDir;
        this.objectMapper = new ObjectMapper();
        log.info("FileDrawingRepository initialized with baseDir={}", baseDir);
    }

    @Override
    public List<Drawing> loadDrawings(String scripId) {
        Path path = resolvePath(scripId);
        if (!Files.exists(path)) return Collections.emptyList();
        try {
            List<DrawingDto> dtos = objectMapper.readValue(path.toFile(), LIST_TYPE);
            log.debug("Loaded {} drawings for {}", dtos.size(), scripId);
            return DrawingDto.toDrawings(dtos);
        } catch (IOException e) {
            log.error("Failed to load drawings for {}", scripId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void saveDrawings(String scripId, List<Drawing> drawings) {
        Path path = resolvePath(scripId);
        try {
            ensureParentExists(path);
            List<DrawingDto> dtos = DrawingDto.fromDrawings(drawings);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), dtos);
            log.debug("Saved {} drawings for {}", dtos.size(), scripId);
        } catch (IOException e) {
            log.error("Failed to save drawings for {}", scripId, e);
        }
    }

    private Path resolvePath(String scripId) {
        String sanitized = scripId.replace(':', File.separatorChar);
        return baseDir.resolve(sanitized).resolve(FILE_NAME);
    }

    private void ensureParentExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
