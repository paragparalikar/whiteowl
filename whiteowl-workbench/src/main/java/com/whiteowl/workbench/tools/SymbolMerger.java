package com.whiteowl.workbench.tools;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges duplicate symbol data created by the first intraday import into the
 * canonical Kite-recognized symbol names, then removes the old directories.
 *
 * <p>The first import used raw CSV symbols (e.g. {@code NIFTY}, {@code BANKNIFTY},
 * {@code CNX-IT}), while Kite uses proper NSE index names (e.g. {@code NIFTY 50},
 * {@code NIFTY BANK}, {@code NIFTY IT}).
 *
 * <p>Run with:
 * <pre>
 *   mvn -pl whiteowl-workbench exec:java \
 *       -Dexec.mainClass="com.whiteowl.workbench.tools.SymbolMerger"
 * </pre>
 */
@Slf4j
public final class SymbolMerger {

    /**
     * Old symbol → Kite-recognized symbol.
     * Order matters only for logging clarity.
     */
    private static final Map<String, String> MERGE_MAP = buildMergeMap();

    private static Map<String, String> buildMergeMap() {
        Map<String, String> m = new LinkedHashMap<>();
        // Index renames
        m.put("NIFTY",              "NIFTY 50");
        m.put("NIFTY-I",            "NIFTY 50");
        m.put("BANKNIFTY",          "NIFTY BANK");
        m.put("BANKNIFTY-I",        "NIFTY BANK");
        m.put("INDIAVIX",           "INDIA VIX");

        // CNX → NIFTY rebrand
        m.put("CNX-IT",             "NIFTY IT");
        m.put("CNXIT",              "NIFTY IT");
        m.put("CNX-MIDCAP",         "NIFTY MIDCAP 50");
        m.put("CNX-NIFTY-JUNIOR",   "NIFTY NEXT 50");
        m.put("CNX100",             "NIFTY 100");
        m.put("CNX500",             "NIFTY 500");
        m.put("CNXENERGY",          "NIFTY ENERGY");
        m.put("CNXFMCG",            "NIFTY FMCG");

        // Spacing / formatting differences
        m.put("NIFTY-MIDCAP50",     "NIFTY MIDCAP 50");
        m.put("NIFTYAUTO",          "NIFTY AUTO");
        m.put("NIFTYFINSERVICE",    "NIFTY FIN SERVICE");
        m.put("NIFTYINFRA",         "NIFTY INFRA");
        m.put("NIFTYMETAL",         "NIFTY METAL");
        return m;
    }

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        FileBarsRepository repo = new FileBarsRepository();
        new SymbolMerger().run(repo);
    }

    // ── Orchestrator ─────────────────────────────────────────────────────

    public void run(FileBarsRepository repo) throws Exception {
        log.info("════════════════════════════════════════════════════════════");
        log.info("  Symbol Merger — START");
        log.info("  Merging {} old symbols into Kite-recognized names", MERGE_MAP.size());
        log.info("════════════════════════════════════════════════════════════");

        int merged = 0;
        int skipped = 0;
        long totalBarsMerged = 0;

        for (Map.Entry<String, String> entry : MERGE_MAP.entrySet()) {
            String oldSymbol = entry.getKey();
            String newSymbol = entry.getValue();
            String oldScripId = "NSE:" + oldSymbol;
            String newScripId = "NSE:" + newSymbol;

            // Only merge ONE_MINUTE — that's what the first import created
            Timeframe tf = Timeframe.ONE_MINUTE;

            if (!repo.exists(oldScripId, tf)) {
                log.info("  SKIP  {} → {} (old data not found)", oldScripId, newScripId);
                skipped++;
                continue;
            }

            Bars oldBars = repo.load(oldScripId, tf);
            int oldCount = oldBars.size();
            if (oldCount == 0) {
                log.info("  SKIP  {} → {} (old data empty)", oldScripId, newScripId);
                skipped++;
                continue;
            }

            int beforeCount = repo.exists(newScripId, tf) ? repo.countBars(newScripId, tf) : 0;

            // Prepend older bars, then append newer bars.
            // Repository handles dedup internally.
            repo.prepend(newScripId, tf, oldBars, 0, oldBars.size());
            repo.append(newScripId, tf, oldBars, 0, oldBars.size());

            int afterCount = repo.countBars(newScripId, tf);
            int newBars = afterCount - beforeCount;

            // Delete old symbol data
            repo.delete(oldScripId, tf);
            deleteEmptyDir(oldScripId, repo);

            log.info("  MERGE {} → {}  |  old={} bars, target {} → {} bars (+{})",
                    oldScripId, newScripId, oldCount, beforeCount, afterCount, newBars);

            merged++;
            totalBarsMerged += newBars;
        }

        log.info("════════════════════════════════════════════════════════════");
        log.info("  Merge COMPLETE");
        log.info("  Merged  : {} symbol pairs", merged);
        log.info("  Skipped : {} (not found or empty)", skipped);
        log.info("  New bars added : {}", totalBarsMerged);
        log.info("════════════════════════════════════════════════════════════");
    }

    /**
     * Remove the now-empty symbol directory after deleting the bin file.
     */
    private void deleteEmptyDir(String scripId, FileBarsRepository repo) {
        try {
            // Resolve the directory path the same way FileBarsRepository does:
            // baseDir / NSE / SYMBOL
            Path baseDir = Path.of(System.getProperty("whiteowl.home",
                    System.getProperty("user.home") + java.io.File.separator + ".whiteowl"),
                    "data", "bars");
            String sanitized = scripId.replace(':', java.io.File.separatorChar);
            Path dir = baseDir.resolve(sanitized);
            if (Files.isDirectory(dir)) {
                // Delete remaining files if any, then the directory
                try (var entries = Files.list(dir)) {
                    entries.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
                }
                Files.deleteIfExists(dir);
                log.debug("Deleted empty directory: {}", dir);
            }
        } catch (IOException e) {
            log.warn("Could not delete old directory for {}: {}", scripId, e.getMessage());
        }
    }
}
