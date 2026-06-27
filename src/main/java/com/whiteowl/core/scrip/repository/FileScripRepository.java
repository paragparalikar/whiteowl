package com.whiteowl.core.scrip.repository;

import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
public final class FileScripRepository implements ScripRepository {

    private static final String FILE_NAME = "scrips.csv";
    private static final String DELIMITER = ",";
    private static final int TOKEN_ID = 0;
    private static final int TOKEN_SYMBOL = 1;
    private static final int TOKEN_NAME = 2;
    private static final int TOKEN_EXCHANGE = 3;
    private static final int TOKEN_SCRIP_TYPE = 4;
    private static final int TOKEN_LOT_SIZE = 5;
    private static final int TOKEN_TICK_SIZE = 6;
    private static final int EXPECTED_TOKEN_COUNT = 7;

    private final Path filePath;
    private final ConcurrentHashMap<String, Scrip> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Scrip> byExchangeSymbol = new ConcurrentHashMap<>();
    private final ScripSearchTrie searchTrie = new ScripSearchTrie();

    public FileScripRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", FILE_NAME));
    }

    public FileScripRepository(Path filePath) {
        this.filePath = filePath;
        loadFromDisk();
        log.info("Loaded {} scrips from {}", byId.size(), filePath);
    }

    @Override
    public Optional<Scrip> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Scrip> findBySymbolAndExchange(String symbol, Exchange exchange) {
        return Optional.ofNullable(byExchangeSymbol.get(compositeKey(exchange, symbol)));
    }

    @Override
    public List<Scrip> findByExchange(Exchange exchange) {
        return byId.values().stream()
                .filter(scrip -> exchange == scrip.getExchange())
                .toList();
    }

    @Override
    public List<Scrip> findByScripType(ScripType scripType) {
        return byId.values().stream()
                .filter(scrip -> scripType == scrip.getScripType())
                .toList();
    }

    @Override
    public List<Scrip> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public List<Scrip> search(String query, int maxResults) {
        return searchTrie.search(query, maxResults);
    }

    @Override
    public boolean matches(Scrip scrip, String query) {
        return searchTrie.matches(scrip, query);
    }

    @Override
    public synchronized void save(Scrip scrip) {
        addToMaps(scrip);
        flushToDisk();
    }

    @Override
    public synchronized void saveAll(List<Scrip> scrips) {
        scrips.forEach(this::addToMaps);
        flushToDisk();
    }

    @Override
    public synchronized void deleteById(String id) {
        Scrip removed = byId.remove(id);
        if (removed != null) {
            byExchangeSymbol.remove(compositeKey(removed.getExchange(), removed.getSymbol()));
            searchTrie.remove(removed);
            flushToDisk();
        }
    }

    private void addToMaps(Scrip scrip) {
        byId.put(scrip.getId(), scrip);
        byExchangeSymbol.put(compositeKey(scrip.getExchange(), scrip.getSymbol()), scrip);
        searchTrie.insert(scrip);
    }

    private String compositeKey(Exchange exchange, String symbol) {
        return exchange.name() + DELIMITER + symbol;
    }

    private void loadFromDisk() {
        if (!Files.exists(filePath)) {
            log.debug("Scrip file not found at {}, starting empty", filePath);
            return;
        }
        try (Stream<String> lines = Files.lines(filePath)){
            lines.filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .forEach(this::addToMaps);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void flushToDisk() {
        try {
            ensureParentExists();
            List<String> lines = byId.values().stream()
                    .map(this::toCsv)
                    .toList();
            Files.write(filePath, lines);
            log.debug("Flushed {} scrips to {}", lines.size(), filePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Scrip parseLine(String line) {
        String[] tokens = line.split(DELIMITER, EXPECTED_TOKEN_COUNT);
        return Scrip.builder()
                .id(tokens[TOKEN_ID])
                .symbol(tokens[TOKEN_SYMBOL])
                .name(tokens[TOKEN_NAME])
                .exchange(Exchange.valueOf(tokens[TOKEN_EXCHANGE]))
                .scripType(ScripType.valueOf(tokens[TOKEN_SCRIP_TYPE]))
                .lotSize(Float.parseFloat(tokens[TOKEN_LOT_SIZE]))
                .tickSize(Float.parseFloat(tokens[TOKEN_TICK_SIZE]))
                .build();
    }

    private String toCsv(Scrip scrip) {
        return String.join(DELIMITER,
                scrip.getId(),
                scrip.getSymbol(),
                scrip.getName(),
                scrip.getExchange().name(),
                scrip.getScripType().name(),
                String.valueOf(scrip.getLotSize()),
                String.valueOf(scrip.getTickSize()));
    }

    private void ensureParentExists() throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
