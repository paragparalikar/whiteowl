package com.whiteowl;

import com.whiteowl.client.kite.adapter.KiteBrokerAdapterFactory;
import com.whiteowl.client.kite.adapter.KiteMapper;
import com.whiteowl.client.kite.adapter.KiteScripLoaderAdapter;
import com.whiteowl.core.account.repository.FileAccountRepository;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.bar.aggregation.LiveDataManager;
import com.whiteowl.core.breadth.BreadthComputer;
import com.whiteowl.core.breadth.BreadthFormulaRegistry;
import com.whiteowl.core.rs.RSComputer;
import com.whiteowl.core.rs.RSFormulaRegistry;
import com.whiteowl.workbench.bar.download.CompositeBarDataDownloader;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.broker.BrokerAdapterFactory;
import com.whiteowl.scripting.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.scripting.examplegroup.repository.FileExampleGroupRepository;
import com.whiteowl.workbench.group.repository.FileGroupRepository;
import com.whiteowl.workbench.group.repository.GroupRepository;
import com.whiteowl.workbench.note.repository.FileNoteRepository;
import com.whiteowl.workbench.note.repository.NoteRepository;
import com.whiteowl.workbench.scrip.download.ScripDataDownloader;
import com.whiteowl.workbench.scrip.migration.ScripMigrationService;
import com.whiteowl.core.scrip.repository.FileScripRepository;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.watchlist.repository.FileWatchlistRepository;
import com.whiteowl.workbench.watchlist.repository.WatchlistRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class Context implements AutoCloseable {

    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final WatchlistRepository watchlistRepository;
    private final GroupRepository groupRepository;
    private final ExampleGroupRepository exampleGroupRepository;
    private final NoteRepository noteRepository;
    private final AccountService accountService;
    private final BrokerAdapterFactory brokerAdapterFactory;
    private final ActiveAccountManager activeAccountManager;
    private final ScripDataDownloader scripDataDownloader;
    private final CompositeBarDataDownloader compositeBarDataDownloader;
    private final LiveDataManager liveDataManager;
    private final BreadthFormulaRegistry breadthFormulaRegistry;
    private final BreadthComputer breadthComputer;
    private final RSFormulaRegistry rsFormulaRegistry;
    private final RSComputer rsComputer;

    public Context() {
        log.info("Initializing WhiteOwl context");
        this.scripRepository = new FileScripRepository();
        this.barsRepository = new FileBarsRepository();
        this.watchlistRepository = new FileWatchlistRepository();
        this.groupRepository = new FileGroupRepository();
        this.exampleGroupRepository = new FileExampleGroupRepository();
        this.noteRepository = new FileNoteRepository();
        this.accountService = new AccountService(new FileAccountRepository());
        this.brokerAdapterFactory = KiteBrokerAdapterFactory.getInstance();
        this.activeAccountManager = new ActiveAccountManager(accountService, brokerAdapterFactory);
        KiteMapper.INSTANCE.warmCache();
        this.scripDataDownloader = new ScripDataDownloader(
                new KiteScripLoaderAdapter(), scripRepository);
        this.scripDataDownloader.setMigrationService(new ScripMigrationService(
                watchlistRepository, groupRepository, exampleGroupRepository, barsRepository));
        this.compositeBarDataDownloader = new CompositeBarDataDownloader(
                brokerAdapterFactory, accountService, scripRepository, barsRepository);
        this.liveDataManager = new LiveDataManager(barsRepository);
        this.breadthFormulaRegistry = new BreadthFormulaRegistry();
        this.breadthComputer = new BreadthComputer(barsRepository);
        this.rsFormulaRegistry = new RSFormulaRegistry();
        this.rsComputer = new RSComputer(barsRepository);
        log.info("WhiteOwl context initialized");
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down WhiteOwl context");
        liveDataManager.close();
        brokerAdapterFactory.closeAll();
        log.info("WhiteOwl context closed");
    }

}
