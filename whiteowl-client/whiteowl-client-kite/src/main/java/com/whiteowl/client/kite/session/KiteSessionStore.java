package com.whiteowl.client.kite.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
public final class KiteSessionStore {

    private static final String FILE_NAME = "session.json";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalTime SESSION_EXPIRY_TIME = LocalTime.of(5, 30);
    private static final Duration MAX_SESSION_AGE = Duration.ofHours(24);

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public KiteSessionStore() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", FILE_NAME));
    }

    public KiteSessionStore(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        log.info("KiteSessionStore initialized with path={}", filePath);
    }

    public Optional<String> loadEnctoken(String username) {
        if (!Files.exists(filePath)) return Optional.empty();
        try {
            KiteSession session = objectMapper.readValue(filePath.toFile(), KiteSession.class);
            if (!username.equals(session.getUsername())) {
                log.info("Stored session belongs to different user, ignoring");
                return Optional.empty();
            }
            if (isExpired(session.getCreatedAt())) {
                log.info("Stored session has expired, clearing");
                clear();
                return Optional.empty();
            }
            log.info("Restored enctoken from stored session for user={}", username);
            return Optional.ofNullable(session.getEnctoken());
        } catch (IOException e) {
            log.warn("Failed to load session from file", e);
            return Optional.empty();
        }
    }

    public void save(String username, String enctoken) {
        try {
            ensureParentExists();
            KiteSession session = KiteSession.builder()
                    .username(username)
                    .enctoken(enctoken)
                    .createdAt(Instant.now().toEpochMilli())
                    .build();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), session);
            log.info("Saved session for user={}", username);
        } catch (IOException e) {
            log.warn("Failed to save session to file", e);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(filePath);
            log.info("Cleared stored session");
        } catch (IOException e) {
            log.warn("Failed to clear session file", e);
        }
    }

    private boolean isExpired(long createdAtMillis) {
        Instant createdAt = Instant.ofEpochMilli(createdAtMillis);
        if (Duration.between(createdAt, Instant.now()).compareTo(MAX_SESSION_AGE) > 0) {
            return true;
        }
        ZonedDateTime createdZdt = createdAt.atZone(IST);
        ZonedDateTime now = ZonedDateTime.now(IST);
        ZonedDateTime todayExpiry = now.toLocalDate().atTime(SESSION_EXPIRY_TIME).atZone(IST);
        return createdZdt.isBefore(todayExpiry) && now.isAfter(todayExpiry);
    }

    private void ensureParentExists() throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
