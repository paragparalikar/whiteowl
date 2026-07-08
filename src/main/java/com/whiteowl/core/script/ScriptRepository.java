package com.whiteowl.core.script;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public final class ScriptRepository {

    private static final String GROOVY_EXTENSION = ".groovy";
    private static final String DATA_DIR = "data";

    private final Path scriptsDir;
    private final ScriptType scriptType;
    private final String defaultTemplate;
    @Getter private final ScriptClassCache classCache = new ScriptClassCache();

    public ScriptRepository(ScriptType scriptType, String directoryName, String defaultTemplate) {
        this(scriptType,
                Paths.get(System.getProperty("whiteowl.home",
                        System.getProperty("user.home") + File.separator + ".whiteowl"),
                        DATA_DIR, directoryName),
                defaultTemplate);
    }

    public ScriptRepository(ScriptType scriptType, Path scriptsDir, String defaultTemplate) {
        this.scriptType = scriptType;
        this.scriptsDir = scriptsDir;
        this.defaultTemplate = defaultTemplate;
        createDirectoryIfNeeded();
    }

    public List<ScriptDescriptor> findAll() {
        if (!Files.exists(scriptsDir)) return Collections.emptyList();
        try (Stream<Path> files = Files.list(scriptsDir)) {
            return files
                    .filter(p -> p.toString().endsWith(GROOVY_EXTENSION))
                    .map(this::toDescriptor)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list scripts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ScriptDescriptor create(String name) throws IOException {
        Path scriptPath = scriptsDir.resolve(sanitizeFileName(name) + GROOVY_EXTENSION);
        Files.writeString(scriptPath, defaultTemplate);
        return ScriptDescriptor.builder()
                .id(name)
                .name(name)
                .type(scriptType)
                .scriptPath(scriptPath)
                .build();
    }

    public ScriptDescriptor rename(ScriptDescriptor descriptor, String newName) throws IOException {
        Path newPath = scriptsDir.resolve(sanitizeFileName(newName) + GROOVY_EXTENSION);
        Files.move(descriptor.getScriptPath(), newPath);
        return ScriptDescriptor.builder()
                .id(newName)
                .name(newName)
                .type(scriptType)
                .scriptPath(newPath)
                .build();
    }

    public void delete(ScriptDescriptor descriptor) throws IOException {
        Files.deleteIfExists(descriptor.getScriptPath());
    }

    public String loadScript(ScriptDescriptor descriptor) throws IOException {
        return Files.readString(descriptor.getScriptPath());
    }

    public void saveScript(ScriptDescriptor descriptor, String content) throws IOException {
        Files.writeString(descriptor.getScriptPath(), content);
    }

    private ScriptDescriptor toDescriptor(Path path) {
        String fileName = path.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - GROOVY_EXTENSION.length());
        return ScriptDescriptor.builder()
                .id(name)
                .name(name)
                .type(scriptType)
                .scriptPath(path)
                .build();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void createDirectoryIfNeeded() {
        try {
            Files.createDirectories(scriptsDir);
        } catch (IOException e) {
            log.error("Failed to create scripts directory: {}", e.getMessage());
        }
    }

}
