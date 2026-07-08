package com.whiteowl.core.collection;

import com.whiteowl.core.examplegroup.model.ExampleGroup;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.group.model.Group;
import com.whiteowl.core.group.repository.GroupRepository;
import com.whiteowl.core.watchlist.model.Watchlist;
import com.whiteowl.core.watchlist.repository.WatchlistRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.whiteowl.core.collection.CollectionType.ALL_SCRIPS;

public final class CollectionResolver {

    private final WatchlistRepository watchlistRepository;
    private final GroupRepository groupRepository;
    private final ExampleGroupRepository exampleGroupRepository;
    private final ActiveTradesResolver activeTradesResolver;

    public CollectionResolver(WatchlistRepository watchlistRepository,
                              GroupRepository groupRepository,
                              ExampleGroupRepository exampleGroupRepository,
                              ActiveTradesResolver activeTradesResolver) {
        this.watchlistRepository = watchlistRepository;
        this.groupRepository = groupRepository;
        this.exampleGroupRepository = exampleGroupRepository;
        this.activeTradesResolver = activeTradesResolver;
    }

    public Set<String> resolve(CollectionSelection selection) {
        if (selection == null || selection.getType() == ALL_SCRIPS) {
            return null;
        }
        return switch (selection.getType()) {
            case WATCHLIST -> resolveWatchlists(selection.getSelectedNames());
            case GROUP -> resolveGroups(selection.getSelectedNames());
            case EXAMPLE_GROUP -> resolveExampleGroups(selection.getSelectedNames());
            case ACTIVE_TRADES -> activeTradesResolver.resolve(selection.getSelectedNames());
            default -> null;
        };
    }

    public List<String> getAvailableNames(CollectionType type) {
        return switch (type) {
            case WATCHLIST -> watchlistRepository.loadAll().stream().map(Watchlist::getName).toList();
            case GROUP -> groupRepository.loadAll().stream().map(Group::getName).toList();
            case EXAMPLE_GROUP -> exampleGroupRepository.loadAll().stream().map(ExampleGroup::getName).toList();
            case ACTIVE_TRADES -> activeTradesResolver.getAvailableNames();
            default -> List.of();
        };
    }

    private Set<String> resolveWatchlists(Set<String> selectedNames) {
        Set<String> ids = new HashSet<>();
        for (Watchlist wl : watchlistRepository.loadAll()) {
            if (selectedNames.isEmpty() || selectedNames.contains(wl.getName())) {
                ids.addAll(wl.getScripIds());
            }
        }
        return ids;
    }

    private Set<String> resolveGroups(Set<String> selectedNames) {
        Set<String> ids = new HashSet<>();
        for (Group group : groupRepository.loadAll()) {
            if (selectedNames.isEmpty() || selectedNames.contains(group.getName())) {
                ids.addAll(group.getScripIds());
            }
        }
        return ids;
    }

    private Set<String> resolveExampleGroups(Set<String> selectedNames) {
        Set<String> ids = new HashSet<>();
        for (ExampleGroup eg : exampleGroupRepository.loadAll()) {
            if (selectedNames.isEmpty() || selectedNames.contains(eg.getName())) {
                eg.getExamples().forEach(ex -> ids.add(ex.getScripId()));
            }
        }
        return ids;
    }

}
