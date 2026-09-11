package com.whiteowl.workbench.scrip.download;

import com.whiteowl.core.broker.ScripLoader;
import com.whiteowl.workbench.scrip.migration.ScripMigrationService;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class ScripDataDownloader {

    private final ScripLoader scripLoader;
    private final ScripRepository scripRepository;
    @Setter private ScripMigrationService migrationService;

    public int download(Exchange exchange) {
        return download(exchange, false);
    }

    public int download(Exchange exchange, boolean forceDownload) {
        log.info("Initiating scrip download for {} (force={})", exchange.name(), forceDownload);
        int count = downloadByExchange(exchange, forceDownload);
        log.info("Finished scrip download for {}", exchange.name());
        return count;
    }

    private int downloadByExchange(Exchange exchange, boolean forceDownload) {
        try {
            List<Scrip> scrips = scripLoader.loadByExchange(exchange, forceDownload);
            List<Scrip> previousScrips = scripRepository.findByExchange(exchange);
            scripRepository.saveAll(scrips);
            log.info("Saved {} scrips for {}", scrips.size(), exchange.name());
            runMigration(previousScrips, scrips);
            return scrips.size();
        } catch (Exception e) {
            log.error("Scrip download failed for {}", exchange.name(), e);
            return 0;
        }
    }

    private void runMigration(List<Scrip> previousScrips, List<Scrip> currentScrips) {
        if (migrationService == null) return;
        try {
            migrationService.migrate(previousScrips, currentScrips);
        } catch (Exception e) {
            log.error("Scrip migration failed", e);
        }
    }

}
