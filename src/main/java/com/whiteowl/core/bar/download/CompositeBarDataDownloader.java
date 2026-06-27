package com.whiteowl.core.bar.download;

import com.whiteowl.client.kite.adapter.BrokerAdapterFactory;
import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
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

    private final AccountService accountService;
    private final BrokerAdapterFactory adapterFactory;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final BarDataDownloader primaryDownloader;
    private final List<BarDataDownloader> activeDownloaders = new ArrayList<>();
    private volatile boolean cancelled;

    public CompositeBarDataDownloader(AccountService accountService,
                                      BrokerAdapterFactory adapterFactory,
                                      ScripRepository scripRepository,
                                      BarsRepository barsRepository,
                                      BarDataDownloader primaryDownloader) {
        this.accountService = accountService;
        this.adapterFactory = adapterFactory;
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
        this.primaryDownloader = primaryDownloader;
    }

    public void download(Collection<Exchange> exchanges, Collection<Timeframe> timeframes,
                         BarDownloadListener listener, Set<ScripType> scripTypes,
                         boolean backwardDownload, Set<String> scripIdFilter) {
        cancelled = false;
        List<Account> accounts = accountService.getAccounts();
        if (accounts.isEmpty()) {
            primaryDownloader.download(exchanges, timeframes, listener, scripTypes,
                    backwardDownload, scripIdFilter);
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
            KiteBrokerAdapter adapter = adapterFactory.createAdapter(account);
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
