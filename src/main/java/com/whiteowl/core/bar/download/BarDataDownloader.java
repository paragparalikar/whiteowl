package com.whiteowl.core.bar.download;

import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
import com.whiteowl.client.kite.model.KiteCandle;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripFilter;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
public final class BarDataDownloader {

    private static final int MAX_RETRY_COUNT = 3;

    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final KiteBrokerAdapter brokerAdapter;
    private final BarDataVerifier verifier;
    private final DownloadQueryTransformer queryTransformer;
    private volatile boolean cancelled;
    private volatile Thread downloadThread;
    private BarDownloadListener listener;
    private boolean backwardEnabled;
    private int forwardBarCount;
    private int backwardBarCount;
    private ZonedDateTime forwardFrom;
    private ZonedDateTime forwardTo;
    private ZonedDateTime backwardFrom;
    private ZonedDateTime backwardTo;

    @Builder
    public BarDataDownloader(ScripRepository scripRepository,
                             BarsRepository barsRepository,
                             KiteBrokerAdapter brokerAdapter) {
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
        this.brokerAdapter = brokerAdapter;
        this.verifier = new BarDataVerifier(barsRepository);
        this.queryTransformer = new DownloadQueryTransformer();
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes) {
        download(exchanges, timeframes, null, null, true);
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes,
                         BarDownloadListener listener) {
        download(exchanges, timeframes, listener, null, true);
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes,
                         BarDownloadListener listener, Set<ScripType> scripTypes,
                         boolean backwardDownload) {
        download(exchanges, timeframes, listener, scripTypes, backwardDownload, null);
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes,
                         BarDownloadListener listener, Set<ScripType> scripTypes,
                         boolean backwardDownload, Set<String> scripIdFilter) {
        this.listener = listener;
        this.backwardEnabled = backwardDownload;
        this.downloadThread = Thread.currentThread();
        cancelled = false;
        try {
            for (Exchange exchange : exchanges) {
                if (cancelled) break;
                List<Scrip> scrips = filterScrips(exchange, scripTypes, scripIdFilter);
                int total = scrips.size();
                int completed = 0;
                for (Timeframe timeframe : timeframes) {
                    if (cancelled) break;
                    log.info("Initiating bar data download for {} {}", exchange.name(), timeframe.name());
                    for (Scrip scrip : scrips) {
                        if (cancelled) break;
                        completed++;
                        notifyScripStart(scrip, timeframe, completed, total);
                        if (isDownloadable(scrip)) {
                            downloadScrip(scrip, timeframe, completed, total);
                        } else {
                            notifyScripComplete(scrip, timeframe, completed, total);
                        }
                    }
                    log.info("Finished downloading data for {} {}", exchange.name(), timeframe.name());
                }
            }
        } finally {
            this.downloadThread = null;
        }
    }

    private List<Scrip> filterScrips(Exchange exchange, Set<ScripType> scripTypes,
                                     Set<String> scripIdFilter) {
        List<Scrip> scrips = scripRepository.findByExchange(exchange);
        if (scripTypes != null && !scripTypes.isEmpty()) {
            scrips = scrips.stream().filter(s -> scripTypes.contains(s.getScripType())).toList();
        }
        if (scripIdFilter != null && !scripIdFilter.isEmpty()) {
            scrips = scrips.stream().filter(s -> scripIdFilter.contains(s.getId())).toList();
        }
        return scrips;
    }

    public void cancel() {
        cancelled = true;
        Thread thread = downloadThread;
        if (thread != null) thread.interrupt();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void downloadSingle(Scrip scrip, Timeframe timeframe,
                                BarDownloadListener listener, boolean backwardDownload) {
        this.listener = listener;
        this.backwardEnabled = backwardDownload;
        if (!isDownloadable(scrip)) return;
        downloadScrip(scrip, timeframe, 0, 0);
    }

    private void downloadScrip(Scrip scrip, Timeframe timeframe, int completed, int total) {
        resetScripState();
        downloadWithRetry(scrip, timeframe, 0);
        notifyForwardComplete(scrip, timeframe);
        notifyBackwardComplete(scrip, timeframe);
        notifyScripComplete(scrip, timeframe, completed, total);
    }

    private void resetScripState() {
        forwardBarCount = 0;
        backwardBarCount = 0;
        forwardFrom = null;
        forwardTo = null;
        backwardFrom = null;
        backwardTo = null;
    }

    private void downloadWithRetry(Scrip scrip, Timeframe timeframe, int attempt) {
        if (cancelled) return;
        if (attempt >= MAX_RETRY_COUNT) {
            log.warn("Max retries exhausted for {} {}", scrip.getSymbol(), timeframe.name());
            return;
        }
        try {
            if (!barsRepository.exists(scrip.getId(), timeframe)) {
                downloadFresh(scrip, timeframe, attempt);
            } else {
                downloadForward(scrip, timeframe, attempt);
                if (!cancelled && backwardEnabled) downloadBackward(scrip, timeframe);
            }
        } catch (Exception e) {
            log.error("Download error for {}", scrip.getSymbol(), e);
            notifyScripError(scrip, timeframe, e.getMessage());
        }
    }

    private void downloadFresh(Scrip scrip, Timeframe timeframe, int attempt) throws IOException {
        ZonedDateTime from = ZonedDateTime.now().minusYears(timeframe.getMaxLookbackYears());
        ZonedDateTime to = ZonedDateTime.now();
        DownloadQuery query = DownloadQuery.builder()
                .scrip(scrip).timeframe(timeframe).from(from).to(to).build();
        queryTransformer.transform(query).ifPresent(q -> executeForwardDownload(q, attempt));
    }

    private void downloadForward(Scrip scrip, Timeframe timeframe, int attempt) throws IOException {
        ZonedDateTime from = resolveForwardFromDate(scrip.getId(), timeframe);
        ZonedDateTime to = ZonedDateTime.now();
        DownloadQuery query = DownloadQuery.builder()
                .scrip(scrip).timeframe(timeframe).from(from).to(to).build();
        queryTransformer.transform(query).ifPresentOrElse(
                q -> executeForwardDownload(q, attempt),
                () -> log.debug("Skipping {} {} forward (already up-to-date)", scrip.getSymbol(), timeframe.name()));
    }

    private void downloadBackward(Scrip scrip, Timeframe timeframe) throws IOException {
        ZonedDateTime maxLookback = ZonedDateTime.now().minusYears(timeframe.getMaxLookbackYears());
        ZonedDateTime to = resolveBackwardToDate(scrip.getId(), timeframe);
        if (to == null || !to.isAfter(maxLookback)) return;
        DownloadQuery query = DownloadQuery.builder()
                .scrip(scrip).timeframe(timeframe).from(maxLookback).to(to).build();
        queryTransformer.transform(query).ifPresentOrElse(
                this::executeBackwardDownload,
                () -> log.debug("Skipping {} {} backward (fully backfilled)", scrip.getSymbol(), timeframe.name()));
    }

    private void executeForwardDownload(DownloadQuery query, int attempt) {
        if (cancelled) return;
        Scrip scrip = query.getScrip();
        Timeframe timeframe = query.getTimeframe();
        try {
            List<KiteCandle> candles = brokerAdapter.fetchHistoricalData(
                    scrip, timeframe, query.getFrom(), query.getTo());
            List<KiteCandle> verified = verifier.verify(scrip.getId(), timeframe, candles);
            appendCandles(scrip.getId(), timeframe, verified);
            forwardBarCount = verified.size();
            forwardFrom = query.getFrom();
            forwardTo = query.getTo();
            log.info("Forward downloaded {} bars for {} {}", verified.size(), scrip.getSymbol(), timeframe.name());
        } catch (InvalidBarDataException e) {
            log.warn("Gap detected for {} {} at {} close={} open={}", scrip.getSymbol(), timeframe.name(),
                    MarketHours.DATE_TIME_FORMAT.format(e.getTimestamp()), e.getPreviousClose(), e.getCurrentOpen());
            deleteAndRetry(scrip, timeframe, attempt);
        }
    }

    private void executeBackwardDownload(DownloadQuery query) {
        if (cancelled) return;
        Scrip scrip = query.getScrip();
        Timeframe timeframe = query.getTimeframe();
        try {
            List<KiteCandle> candles = brokerAdapter.fetchHistoricalData(
                    scrip, timeframe, query.getFrom(), query.getTo());
            if (candles.isEmpty()) return;
            prependCandles(scrip.getId(), timeframe, candles);
            backwardBarCount = candles.size();
            backwardFrom = query.getFrom();
            backwardTo = query.getTo();
            log.info("Backward downloaded {} bars for {} {}", candles.size(), scrip.getSymbol(), timeframe.name());
        } catch (Exception e) {
            log.error("Backward download failed for {} {}", scrip.getSymbol(), timeframe.name(), e);
        }
    }

    private boolean isDownloadable(Scrip scrip) {
        return ScripFilter.isTradable(scrip) && brokerAdapter.hasInstrumentMapping(scrip);
    }

    private void deleteAndRetry(Scrip scrip, Timeframe timeframe, int attempt) {
        try {
            barsRepository.delete(scrip.getId(), timeframe);
        } catch (IOException e) {
            log.error("Delete failed for {} {}", scrip.getSymbol(), timeframe.name(), e);
        }
        downloadWithRetry(scrip, timeframe, attempt + 1);
    }

    private ZonedDateTime resolveForwardFromDate(String scripId, Timeframe timeframe) throws IOException {
        return barsRepository.findLatestTimestamp(scripId, timeframe)
                .map(lastTs -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastTs), ZoneId.systemDefault()))
                .orElse(ZonedDateTime.now().minusYears(timeframe.getMaxLookbackYears()));
    }

    private ZonedDateTime resolveBackwardToDate(String scripId, Timeframe timeframe) throws IOException {
        return barsRepository.findEarliestTimestamp(scripId, timeframe)
                .map(firstTs -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(firstTs), ZoneId.systemDefault())
                        .minusSeconds(timeframe.getSeconds()))
                .orElse(null);
    }

    private void appendCandles(String scripId, Timeframe timeframe, List<KiteCandle> candles) {
        if (candles.isEmpty()) return;
        Bars bars = toCandleBars(scripId, timeframe, candles);
        try {
            if (barsRepository.exists(scripId, timeframe)) {
                barsRepository.append(scripId, timeframe, bars, 0, bars.size());
            } else {
                barsRepository.save(scripId, timeframe, bars);
            }
        } catch (IOException e) {
            log.error("Append failed for {} {}", scripId, timeframe.name(), e);
        }
    }

    private void prependCandles(String scripId, Timeframe timeframe, List<KiteCandle> candles) {
        if (candles.isEmpty()) return;
        Bars bars = toCandleBars(scripId, timeframe, candles);
        try {
            barsRepository.prepend(scripId, timeframe, bars, 0, bars.size());
        } catch (IOException e) {
            log.error("Prepend failed for {} {}", scripId, timeframe.name(), e);
        }
    }

    private Bars toCandleBars(String scripId, Timeframe timeframe, List<KiteCandle> candles) {
        Bars bars = new Bars(scripId, timeframe, candles.size());
        for (KiteCandle candle : candles) {
            bars.append(candle.getTimestamp(), candle.getOpen(), candle.getHigh(),
                    candle.getLow(), candle.getClose(), candle.getVolume());
        }
        return bars;
    }

    private void notifyScripStart(Scrip scrip, Timeframe timeframe, int completed, int total) {
        if (listener != null) listener.onScripStart(scrip, timeframe, completed, total);
    }

    private void notifyForwardComplete(Scrip scrip, Timeframe timeframe) {
        if (listener != null && forwardBarCount > 0) {
            listener.onForwardComplete(scrip, timeframe, forwardBarCount, forwardFrom, forwardTo);
        }
    }

    private void notifyBackwardComplete(Scrip scrip, Timeframe timeframe) {
        if (listener != null && backwardBarCount > 0) {
            listener.onBackwardComplete(scrip, timeframe, backwardBarCount, backwardFrom, backwardTo);
        }
    }

    private void notifyScripComplete(Scrip scrip, Timeframe timeframe, int completed, int total) {
        if (listener != null) listener.onScripComplete(scrip, timeframe, completed, total);
    }

    private void notifyScripError(Scrip scrip, Timeframe timeframe, String error) {
        if (listener != null) listener.onScripError(scrip, timeframe, error);
    }

}
