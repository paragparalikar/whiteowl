package com.whiteowl.workbench.scrip.migration;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.scripting.examplegroup.model.Example;
import com.whiteowl.scripting.examplegroup.model.ExampleGroup;
import com.whiteowl.scripting.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.workbench.watchlist.model.Watchlist;
import com.whiteowl.workbench.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class ScripMigrationService {

    private final WatchlistRepository watchlistRepository;
    private final GroupRepository groupRepository;
    private final ExampleGroupRepository exampleGroupRepository;
    private final BarsRepository barsRepository;

    public void migrate(List<Scrip> previousScrips, List<Scrip> currentScrips) {
        Map<String, String> renames = detectRenames(previousScrips, currentScrips);
        if (renames.isEmpty()) {
            log.debug("No scrip renames detected");
            return;
        }
        log.info("Detected {} scrip rename(s): {}", renames.size(), renames);
        migrateWatchlists(renames);
        migrateGroups(renames);
        migrateExampleGroups(renames);
        migrateBarData(renames);
    }

    Map<String, String> detectRenames(List<Scrip> previousScrips, List<Scrip> currentScrips) {
        Set<String> currentIds = currentScrips.stream()
                .map(Scrip::getId)
                .collect(Collectors.toSet());
        Map<String, Scrip> currentByNameExchange = new HashMap<>();
        for (Scrip scrip : currentScrips) {
            if (scrip.getName() != null && scrip.getExchange() != null) {
                currentByNameExchange.put(composeKey(scrip), scrip);
            }
        }
        Map<String, String> renames = new HashMap<>();
        for (Scrip old : previousScrips) {
            if (currentIds.contains(old.getId())) continue;
            if (old.getName() == null || old.getExchange() == null) continue;
            Scrip match = currentByNameExchange.get(composeKey(old));
            if (match != null) {
                renames.put(old.getId(), match.getId());
                log.info("Detected scrip rename: {} -> {} (name={})", old.getId(), match.getId(), old.getName());
            }
        }
        return renames;
    }

    private void migrateWatchlists(Map<String, String> renames) {
        List<Watchlist> watchlists = watchlistRepository.loadAll();
        boolean changed = false;
        for (Watchlist watchlist : watchlists) {
            if (replaceScripIds(watchlist.getScripIds(), renames)) {
                log.info("Migrated watchlist '{}'", watchlist.getName());
                changed = true;
            }
        }
        if (changed) {
            watchlistRepository.saveAll(watchlists);
        }
    }

    private void migrateGroups(Map<String, String> renames) {
        List<Group> groups = groupRepository.loadAll();
        boolean changed = false;
        for (Group group : groups) {
            if (replaceScripIds(group.getScripIds(), renames)) {
                log.info("Migrated group '{}'", group.getName());
                changed = true;
            }
        }
        if (changed) {
            groupRepository.saveAll(groups);
        }
    }

    private void migrateExampleGroups(Map<String, String> renames) {
        List<ExampleGroup> groups = exampleGroupRepository.loadAll();
        boolean changed = false;
        for (ExampleGroup group : groups) {
            if (replaceExampleScripIds(group, renames)) {
                log.info("Migrated example group '{}'", group.getName());
                changed = true;
            }
        }
        if (changed) {
            exampleGroupRepository.saveAll(groups);
        }
    }

    private void migrateBarData(Map<String, String> renames) {
        for (Map.Entry<String, String> entry : renames.entrySet()) {
            try {
                barsRepository.rename(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                log.error("Failed to rename bar data from {} to {}", entry.getKey(), entry.getValue(), e);
            }
        }
    }

    private boolean replaceScripIds(List<String> scripIds, Map<String, String> renames) {
        boolean changed = false;
        for (int i = 0; i < scripIds.size(); i++) {
            String newId = renames.get(scripIds.get(i));
            if (newId != null) {
                scripIds.set(i, newId);
                changed = true;
            }
        }
        return changed;
    }

    private boolean replaceExampleScripIds(ExampleGroup group, Map<String, String> renames) {
        List<Example> examples = group.getExamples();
        boolean changed = false;
        for (int i = 0; i < examples.size(); i++) {
            Example example = examples.get(i);
            String newId = renames.get(example.getScripId());
            if (newId != null) {
                examples.set(i, new Example(newId, example.getTimeframe(),
                        example.getStartTimestamp(), example.getEndTimestamp()));
                changed = true;
            }
        }
        return changed;
    }

    private static String composeKey(Scrip scrip) {
        return scrip.getExchange().name() + ":" + scrip.getName().toUpperCase();
    }

}
