package com.whiteowl.core.scrip.download;

import com.whiteowl.client.kite.adapter.KiteMapper;
import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.client.kite.symbol.KiteFileSystemScripLoader;
import com.whiteowl.client.kite.symbol.KiteScripLoader;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class ScripDataDownloader {

    private final KiteScripLoader scripLoader;
    private final ScripRepository scripRepository;
    private final KiteMapper mapper;

    public ScripDataDownloader(ScripRepository scripRepository) {
        this(new KiteFileSystemScripLoader(), scripRepository, KiteMapper.INSTANCE);
    }

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
            KiteExchange kiteExchange = mapper.toKiteExchange(exchange);
            List<KiteSymbol> kiteSymbols = (scripLoader instanceof KiteFileSystemScripLoader fsLoader)
                    ? fsLoader.loadByExchange(kiteExchange, forceDownload)
                    : scripLoader.loadByExchange(kiteExchange);
            List<Scrip> scrips = kiteSymbols.stream()
                    .map(mapper::toScrip)
                    .toList();
            scripRepository.saveAll(scrips);
            log.info("Saved {} scrips for {}", scrips.size(), exchange.name());
            return scrips.size();
        } catch (Exception e) {
            log.error("Scrip download failed for {}", exchange.name(), e);
            return 0;
        }
    }

}
