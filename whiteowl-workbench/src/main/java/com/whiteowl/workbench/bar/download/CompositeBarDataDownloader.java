package com.whiteowl.workbench.bar.download;

import com.whiteowl.core.broker.BrokerAdapterFactory;
import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class CompositeBarDataDownloader {

    private final BrokerAdapterFactory brokerAdapterFactory;
    private final AccountService accountService;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final List<BarDataDownloader> activeDownloaders = new ArrayList<>();
    private volatile boolean cancelled;

    public CompositeBarDataDownloader(BrokerAdapterFactory brokerAdapterFactory,
                                      AccountService accountService,
                                      ScripRepository scripRepository,
                                      BarsRepository barsRepository) {
        this.brokerAdapterFactory = brokerAdapterFactory;
        this.accountService = accountService;
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes,
                         BarDownloadListener listener, Set<ScripType> scripTypes,
                         boolean backwardDownload, Set<String> scripIdFilter) {
        cancelled = false;
        List<Account> accounts = accountService.getAccounts();
        if (accounts.isEmpty()) {
            log.warn("No accounts configured — skipping bar data download");
            return;
        }
        List<BarDataDownloader> downloaders = buildDownloaders(accounts);
        synchronized (activeDownloaders) {
            activeDownloaders.clear();
            activeDownloaders.addAll(downloaders);
        }
        for (Exchange exchange : exchanges) {
            if (cancelled) break;
            List<Scrip> scrips = filterScrips(exchange, scripTypes, scripIdFilter);
            for (Timeframe timeframe : timeframes) {
                if (cancelled) break;
                log.info("Initiating bar data download for {} {}", exchange.name(), timeframe.name());
                downloadWithQueue(scrips, timeframe, downloaders, listener, backwardDownload);
                log.info("Finished downloading data for {} {}", exchange.name(), timeframe.name());
            }
        }
    }

    public void cancel() {
        cancelled = true;
        synchronized (activeDownloaders) {
            for (BarDataDownloader downloader : activeDownloaders) {
                downloader.cancel();
            }
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private void downloadWithQueue(List<Scrip> scrips, Timeframe timeframe,
                                    List<BarDataDownloader> downloaders,
                                    BarDownloadListener listener, boolean backwardDownload) {
        ConcurrentLinkedQueue<Scrip> queue = new ConcurrentLinkedQueue<>(scrips);
        int total = scrips.size();
        AtomicInteger completed = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < downloaders.size(); i++) {
            BarDataDownloader downloader = downloaders.get(i);
            Thread thread = new Thread(() -> processQueue(queue, downloader, timeframe,
                    listener, backwardDownload, completed, total));
            thread.setDaemon(true);
            thread.setName("bar-download-worker-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
            }
        }
    }

    private void processQueue(ConcurrentLinkedQueue<Scrip> queue, BarDataDownloader downloader,
                               Timeframe timeframe, BarDownloadListener listener,
                               boolean backwardDownload, AtomicInteger completed, int total) {
        Scrip scrip;
        while (!cancelled && (scrip = queue.poll()) != null) {
            try {
                int done = completed.incrementAndGet();
                if (listener != null) listener.onScripStart(scrip, timeframe, done, total);
                downloader.downloadSingle(scrip, timeframe, listener, backwardDownload);
            } catch (Exception e) {
                log.error("Download failed for {} {}: {}", scrip.getSymbol(), timeframe.name(), e.getMessage());
                if (listener != null) listener.onScripError(scrip, timeframe, e.getMessage());
            }
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

    private List<BarDataDownloader> buildDownloaders(List<Account> accounts) {
        List<BarDataDownloader> downloaders = new ArrayList<>();
        for (Account account : accounts) {
            BrokerAdapter adapter = brokerAdapterFactory.createAdapter(account);
            BarDataDownloader downloader = BarDataDownloader.builder()
                    .scripRepository(scripRepository)
                    .barsRepository(barsRepository)
                    .brokerAdapter(adapter)
                    .build();
            downloaders.add(downloader);
        }
        return downloaders;
    }

}
