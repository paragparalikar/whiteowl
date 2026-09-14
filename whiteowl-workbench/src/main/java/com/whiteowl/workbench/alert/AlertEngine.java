package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.aggregation.BarCompletionListener;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.scripting.screener.Screen;
import com.whiteowl.scripting.screener.ScreenRegistry;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public final class AlertEngine implements BarCompletionListener {

    private final AlertRepository alertRepository;
    private final GroupRepository groupRepository;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final ScreenRegistry screenRegistry;
    private final List<AlertDefinition> alertDefinitions;
    private final Map<String, ObservableList<AlertMatch>> alertMatches;
    private final Map<String, Set<String>> lastMatchedScrips;
    private final List<AlertMatchListener> matchListeners;

    public AlertEngine(AlertRepository alertRepository, GroupRepository groupRepository,
                      ScripRepository scripRepository, BarsRepository barsRepository,
                      ScreenRegistry screenRegistry) {
        this.alertRepository = alertRepository;
        this.groupRepository = groupRepository;
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
        this.screenRegistry = screenRegistry;
        this.alertDefinitions = new CopyOnWriteArrayList<>(alertRepository.loadAll());
        this.alertMatches = new ConcurrentHashMap<>();
        this.lastMatchedScrips = new ConcurrentHashMap<>();
        this.matchListeners = new CopyOnWriteArrayList<>();
        initializeAlertMatches();
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
        Group group = findGroupById(alertDefinition.getGroupId());
        if (group == null || !group.containsScrip(scripId)) return;

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

    private Group findGroupById(String groupId) {
        return groupRepository.loadAll().stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElse(null);
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
    }

    public void updateAlertDefinition(AlertDefinition alertDefinition) {
        alertRepository.save(alertDefinition);
        int index = alertDefinitions.indexOf(alertDefinition);
        if (index >= 0) {
            alertDefinitions.set(index, alertDefinition);
        }
        clearAlertMatches(alertDefinition.getId());
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
