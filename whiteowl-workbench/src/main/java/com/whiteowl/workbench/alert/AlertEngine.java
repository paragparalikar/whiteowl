package com.whiteowl.workbench.alert;

import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.bar.aggregation.BarCompletionListener;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.scripting.screener.Screen;
import com.whiteowl.scripting.screener.ScreenRegistry;
import com.whiteowl.workbench.bar.download.BarGapBackfillService;
import com.whiteowl.workbench.collection.CollectionType;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import com.whiteowl.workbench.watchlist.model.Watchlist;
import com.whiteowl.workbench.watchlist.repository.WatchlistRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class AlertEngine implements BarCompletionListener {

    private static final long RECONCILE_INTERVAL_SECONDS = 30;

    private final AlertRepository alertRepository;
    private final GroupRepository groupRepository;
    private final WatchlistRepository watchlistRepository;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final ScreenRegistry screenRegistry;
    private final BarGapBackfillService backfillService;
    private final ActiveAccountManager activeAccountManager;
    private final List<AlertDefinition> alertDefinitions;
    private final Map<String, ObservableList<AlertMatch>> alertMatches;
    private final Map<String, Set<String>> lastMatchedScrips;
    private final List<AlertMatchListener> matchListeners;
    private final Set<String> subscribedScripIds = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconcileExecutor;

    public AlertEngine(AlertRepository alertRepository, GroupRepository groupRepository,
                      WatchlistRepository watchlistRepository,
                      ScripRepository scripRepository, BarsRepository barsRepository,
                      ScreenRegistry screenRegistry,
                      BarGapBackfillService backfillService,
                      ActiveAccountManager activeAccountManager) {
        this.alertRepository = alertRepository;
        this.groupRepository = groupRepository;
        this.watchlistRepository = watchlistRepository;
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
        this.screenRegistry = screenRegistry;
        this.backfillService = backfillService;
        this.activeAccountManager = activeAccountManager;
        this.alertDefinitions = new CopyOnWriteArrayList<>(alertRepository.loadAll());
        this.alertMatches = new ConcurrentHashMap<>();
        this.lastMatchedScrips = new ConcurrentHashMap<>();
        this.matchListeners = new CopyOnWriteArrayList<>();
        initializeAlertMatches();
        this.reconcileExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-engine-reconcile");
            t.setDaemon(true);
            return t;
        });
        reconcileExecutor.scheduleWithFixedDelay(this::reconcile,
                RECONCILE_INTERVAL_SECONDS, RECONCILE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        reconcile();
    }

    public void shutdown() {
        reconcileExecutor.shutdownNow();
    }

    /**
     * Ensures every scrip in every enabled alert's group has:
     *   1) historical bars backfilled up to now for 1m + alert timeframe
     *   2) an active live tick subscription so LiveDataManager produces bars
     * Runs periodically to pick up changes to group membership without needing
     * explicit event wiring from GroupPane/WatchlistPane.
     */
    private void reconcile() {
        try {
            Map<String, Set<Timeframe>> scripToTimeframes = new HashMap<>();
            for (AlertDefinition ad : alertDefinitions) {
                if (!ad.isEnabled()) continue;
                for (String scripId : resolveSourceScripIds(ad)) {
                    scripToTimeframes.computeIfAbsent(scripId, k -> new HashSet<>())
                            .add(ad.getTimeframe());
                }
            }
            if (scripToTimeframes.isEmpty()) return;
            BrokerAdapter adapter = activeAccountManager.getActiveAdapter().orElse(null);
            List<Scrip> toSubscribe = new ArrayList<>();
            for (Map.Entry<String, Set<Timeframe>> entry : scripToTimeframes.entrySet()) {
                String scripId = entry.getKey();
                Optional<Scrip> scripOpt = scripRepository.findById(scripId);
                if (scripOpt.isEmpty()) continue;
                Scrip scrip = scripOpt.get();
                List<Timeframe> tfs = new ArrayList<>(entry.getValue());
                if (!tfs.contains(Timeframe.ONE_MINUTE)) tfs.add(Timeframe.ONE_MINUTE);
                if (backfillService != null) {
                    backfillService.backfill(scrip, tfs);
                }
                if (adapter != null && subscribedScripIds.add(scripId)
                        && adapter.hasInstrumentMapping(scrip)) {
                    toSubscribe.add(scrip);
                }
            }
            if (adapter != null && !toSubscribe.isEmpty()) {
                adapter.subscribe(toSubscribe);
                log.info("AlertEngine subscribed {} scrip(s) for live ticks", toSubscribe.size());
            }
        } catch (Exception e) {
            log.warn("AlertEngine reconcile failed", e);
        }
    }

    private void initializeAlertMatches() {
        for (AlertDefinition alertDefinition : alertDefinitions) {
            alertMatches.put(alertDefinition.getId(), FXCollections.observableArrayList());
            lastMatchedScrips.put(alertDefinition.getId(), new HashSet<>());
        }
    }

    public void refreshAlertDefinitions() {
        alertDefinitions.clear();
        alertDefinitions.addAll(alertRepository.loadAll());
        for (AlertDefinition alertDefinition : alertDefinitions) {
            if (!alertMatches.containsKey(alertDefinition.getId())) {
                alertMatches.put(alertDefinition.getId(), FXCollections.observableArrayList());
                lastMatchedScrips.put(alertDefinition.getId(), new HashSet<>());
            }
        }
    }

    public ObservableList<AlertMatch> getAlertMatches(String alertDefinitionId) {
        return alertMatches.getOrDefault(alertDefinitionId, FXCollections.observableArrayList());
    }

    public void addAlertMatchListener(AlertMatchListener listener) {
        matchListeners.add(listener);
    }

    public void removeAlertMatchListener(AlertMatchListener listener) {
        matchListeners.remove(listener);
    }

    public void clearAlertMatches(String alertDefinitionId) {
        ObservableList<AlertMatch> matches = alertMatches.get(alertDefinitionId);
        if (matches != null) {
            matches.clear();
            lastMatchedScrips.get(alertDefinitionId).clear();
        }
    }

    public void clearAllAlertMatches() {
        alertMatches.values().forEach(ObservableList::clear);
        lastMatchedScrips.values().forEach(Set::clear);
    }

    @Override
    public void onBarCompleted(String scripId, Timeframe timeframe, long timestamp,
                               float open, float high, float low, float close, long volume) {
        for (AlertDefinition alertDefinition : alertDefinitions) {
            if (!alertDefinition.isEnabled()) continue;
            if (alertDefinition.getTimeframe() != timeframe) continue;
            evaluateAlert(alertDefinition, scripId);
        }
    }

    private void evaluateAlert(AlertDefinition alertDefinition, String scripId) {
        if (!resolveSourceScripIds(alertDefinition).contains(scripId)) return;

        Screen screen = findScreenById(alertDefinition.getScreenId());
        if (screen == null) return;

        try {
            Bars bars = barsRepository.load(scripId, alertDefinition.getTimeframe());
            if (bars == null || bars.size() == 0) return;

            Optional<Scrip> scrip = scripRepository.findById(scripId);
            if (scrip.isEmpty()) return;

            boolean matches = screen.matches(scrip.get(), bars);
            Set<String> lastMatched = lastMatchedScrips.get(alertDefinition.getId());

            if (matches) {
                if (!lastMatched.contains(scripId)) {
                    AlertMatch alertMatch = new AlertMatch(alertDefinition, scrip.get());
                    ObservableList<AlertMatch> matchesList = alertMatches.get(alertDefinition.getId());
                    matchesList.add(alertMatch);
                    lastMatched.add(scripId);
                    notifyMatchListeners(alertMatch);
                    log.debug("Alert '{}' matched for scrip {}", alertDefinition.getName(), scripId);
                }
            } else {
                lastMatched.remove(scripId);
            }
        } catch (IOException e) {
            log.warn("Failed to evaluate alert {} for scrip {}", alertDefinition.getName(), scripId, e);
        }
    }

    private List<String> resolveSourceScripIds(AlertDefinition ad) {
        CollectionType type = ad.getSourceType() != null ? ad.getSourceType() : CollectionType.GROUP;
        String sourceId = ad.getSourceId();
        if (sourceId == null) return List.of();
        if (type == CollectionType.WATCHLIST) {
            return watchlistRepository.loadAll().stream()
                    .filter(w -> sourceId.equals(w.getId()))
                    .findFirst()
                    .map(Watchlist::getScripIds)
                    .orElse(List.of());
        }
        return groupRepository.loadAll().stream()
                .filter(g -> sourceId.equals(g.getId()))
                .findFirst()
                .map(Group::getScripIds)
                .orElse(List.of());
    }

    private Screen findScreenById(String screenId) {
        return screenRegistry.getScreens().stream()
                .filter(s -> s.getId().equals(screenId))
                .findFirst()
                .orElse(null);
    }

    private void notifyMatchListeners(AlertMatch alertMatch) {
        for (AlertMatchListener listener : matchListeners) {
            try {
                listener.onAlertMatch(alertMatch);
            } catch (Exception e) {
                log.warn("Error in alert match listener", e);
            }
        }
    }

    public List<AlertDefinition> getAlertDefinitions() {
        return new ArrayList<>(alertDefinitions);
    }

    public void addAlertDefinition(AlertDefinition alertDefinition) {
        alertDefinitions.add(alertDefinition);
        alertRepository.save(alertDefinition);
        alertMatches.put(alertDefinition.getId(), FXCollections.observableArrayList());
        lastMatchedScrips.put(alertDefinition.getId(), new HashSet<>());
        reconcile();
    }

    public void updateAlertDefinition(AlertDefinition alertDefinition) {
        alertRepository.save(alertDefinition);
        int index = alertDefinitions.indexOf(alertDefinition);
        if (index >= 0) {
            alertDefinitions.set(index, alertDefinition);
        }
        clearAlertMatches(alertDefinition.getId());
        reconcile();
    }

    public void deleteAlertDefinition(String alertDefinitionId) {
        alertDefinitions.removeIf(a -> a.getId().equals(alertDefinitionId));
        alertRepository.delete(alertDefinitionId);
        alertMatches.remove(alertDefinitionId);
        lastMatchedScrips.remove(alertDefinitionId);
    }

    public void moveAlertMatch(String alertDefinitionId, int fromIndex, int toIndex) {
        ObservableList<AlertMatch> matches = alertMatches.get(alertDefinitionId);
        if (matches != null && fromIndex >= 0 && fromIndex < matches.size() && toIndex >= 0 && toIndex < matches.size()) {
            AlertMatch moved = matches.remove(fromIndex);
            matches.add(toIndex, moved);
        }
    }

    public void moveAlertMatchToTop(String alertDefinitionId, int fromIndex) {
        ObservableList<AlertMatch> matches = alertMatches.get(alertDefinitionId);
        if (matches != null && fromIndex >= 0 && fromIndex < matches.size()) {
            AlertMatch moved = matches.remove(fromIndex);
            matches.add(0, moved);
        }
    }

    public void moveAlertMatchToBottom(String alertDefinitionId, int fromIndex) {
        ObservableList<AlertMatch> matches = alertMatches.get(alertDefinitionId);
        if (matches != null && fromIndex >= 0 && fromIndex < matches.size()) {
            AlertMatch moved = matches.remove(fromIndex);
            matches.add(moved);
        }
    }
}
