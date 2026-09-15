package com.whiteowl.workbench.watchlist.model;

import com.whiteowl.workbench.collection.NamedScripCollection;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public final class Watchlist implements NamedScripCollection {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    private final String id;
    @Setter private String name;
    private final List<String> scripIds;

    public Watchlist(String name) {
        validateName(name);
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.scripIds = new ArrayList<>();
    }

    public Watchlist(String name, List<String> scripIds) {
        validateName(name);
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.scripIds = new ArrayList<>(scripIds);
    }

    public Watchlist(String id, String name, List<String> scripIds) {
        validateName(name);
        this.id = id;
        this.name = name;
        this.scripIds = new ArrayList<>(scripIds);
    }

    public void addScrip(String scripId) {
        if (!scripIds.contains(scripId)) {
            scripIds.add(scripId);
        }
    }

    public void removeScrip(String scripId) {
        scripIds.remove(scripId);
    }

    public boolean containsScrip(String scripId) {
        return scripIds.contains(scripId);
    }

    public void moveScrip(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= scripIds.size()) return;
        if (toIndex < 0 || toIndex >= scripIds.size()) return;
        String scripId = scripIds.remove(fromIndex);
        scripIds.add(toIndex, scripId);
    }

    private static void validateName(String name) {
        if (name == null || name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Watchlist name must be between 1 and 255 characters");
        }
    }

}
