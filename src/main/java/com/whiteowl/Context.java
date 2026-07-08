package com.whiteowl;

import com.whiteowl.client.kite.KiteApi;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteLazyApi;
import com.whiteowl.client.kite.adapter.BrokerAdapterFactory;
import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
import com.whiteowl.client.kite.adapter.KiteMapper;
import com.whiteowl.core.account.repository.FileAccountRepository;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.bar.download.BarDataDownloader;
import com.whiteowl.core.bar.download.CompositeBarDataDownloader;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.examplegroup.repository.FileExampleGroupRepository;
import com.whiteowl.core.group.repository.FileGroupRepository;
import com.whiteowl.core.group.repository.GroupRepository;
import com.whiteowl.core.note.repository.FileNoteRepository;
import com.whiteowl.core.note.repository.NoteRepository;
import com.whiteowl.core.scrip.download.ScripDataDownloader;
import com.whiteowl.core.scrip.repository.FileScripRepository;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.core.watchlist.repository.FileWatchlistRepository;
import com.whiteowl.core.watchlist.repository.WatchlistRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@Getter
public final class Context implements AutoCloseable {

    private final KiteApi kiteApi;
    private final KiteBrokerAdapter brokerAdapter;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final WatchlistRepository watchlistRepository;
    private final GroupRepository groupRepository;
    private final ExampleGroupRepository exampleGroupRepository;
    private final NoteRepository noteRepository;
    private final AccountService accountService;
    private final ActiveAccountManager activeAccountManager;
    private final ScripDataDownloader scripDataDownloader;
    private final BarDataDownloader barDataDownloader;
    private final CompositeBarDataDownloader compositeBarDataDownloader;

    public Context(KiteCredentials kiteCredentials) {
        this(() -> kiteCredentials, kiteCredentials.getPortfolioId());
    }

    public Context(Supplier<KiteCredentials> credentialsSupplier, String portfolioId) {
        log.info("Initializing WhiteOwl context");
        BrokerAdapterFactory brokerAdapterFactory = BrokerAdapterFactory.getInstance();
        this.kiteApi = new KiteLazyApi(credentialsSupplier, brokerAdapterFactory.getSessionStore());
        this.brokerAdapter = new KiteBrokerAdapter(kiteApi, portfolioId);
        this.scripRepository = new FileScripRepository();
        this.barsRepository = new FileBarsRepository();
        this.watchlistRepository = new FileWatchlistRepository();
        this.groupRepository = new FileGroupRepository();
        this.exampleGroupRepository = new FileExampleGroupRepository();
        this.noteRepository = new FileNoteRepository();
        this.accountService = new AccountService(new FileAccountRepository());
        this.activeAccountManager = new ActiveAccountManager(accountService);
        KiteMapper.INSTANCE.warmCache();
        this.scripDataDownloader = new ScripDataDownloader(scripRepository);
        this.barDataDownloader = BarDataDownloader.builder()
                .scripRepository(scripRepository)
                .barsRepository(barsRepository)
                .brokerAdapter(brokerAdapter)
                .build();
        this.compositeBarDataDownloader = new CompositeBarDataDownloader(
                accountService, scripRepository, barsRepository, barDataDownloader);
        log.info("WhiteOwl context initialized (broker API deferred)");
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down WhiteOwl context");
        BrokerAdapterFactory.getInstance().closeAll();
        kiteApi.close();
        log.info("WhiteOwl context closed");
    }

}
