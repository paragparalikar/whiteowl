package com.whiteowl.client.kite.symbol;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class KiteFileSystemScripLoader implements KiteScripLoader {

    private static final String HOME = System.getProperty("whiteowl.home",
            System.getProperty("user.home") + File.separator + ".whiteowl");
    private static final String SYMBOL_DIR = "kite" + File.separator + "symbols";
    private static final String FILE_EXTENSION = ".csv";
    private static final String HYPHEN = "-";

    private final KiteScripLoader delegate;

    public KiteFileSystemScripLoader() {
        this(new KiteHttpScripLoader());
    }

    @Override
    @SneakyThrows
    public List<KiteSymbol> loadByExchange(KiteExchange exchange) {
        return loadByExchange(exchange, false);
    }

    @SneakyThrows
    public List<KiteSymbol> loadByExchange(KiteExchange exchange, boolean forceDownload) {
        Path path = resolvePath(exchange);
        if (!forceDownload && isCacheValid(path)) {
            log.debug("Loading symbols from cache for {}", exchange.name());
            return loadFromFile(path);
        }
        log.debug("Downloading fresh symbols for {}", exchange.name());
        return downloadAndCache(exchange, path);
    }

    private Path resolvePath(KiteExchange exchange) {
        return Paths.get(HOME, SYMBOL_DIR, exchange.name().toLowerCase() + FILE_EXTENSION);
    }

    private boolean isCacheValid(Path path) {
        if (!Files.exists(path)) return false;
        try {
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
            return lastModified.isAfter(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    private List<KiteSymbol> loadFromFile(Path path) {
        return Files.lines(path)
                .filter(line -> !line.contains(HYPHEN))
                .map(KiteSymbol::parseCsv)
                .toList();
    }

    @SneakyThrows
    private List<KiteSymbol> downloadAndCache(KiteExchange exchange, Path path) {
        List<KiteSymbol> symbols = delegate.loadByExchange(exchange);
        ensureDirectoryExists(path);
        List<String> lines = symbols.stream()
                .filter(symbol -> !symbol.getTradingsymbol().contains(HYPHEN))
                .map(KiteSymbol::toCsv)
                .toList();
        Files.write(path, lines);
        log.debug("Cached {} symbols to {}", lines.size(), path);
        return symbols;
    }

    @SneakyThrows
    private void ensureDirectoryExists(Path path) {
        Path parent = path.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
