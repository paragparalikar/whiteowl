package com.whiteowl.workbench.group.model;

import com.whiteowl.workbench.collection.NamedScripCollection;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class Group implements NamedScripCollection {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    @Setter private String name;
    private final List<String> scripIds;

    public Group(String name) {
        validateName(name);
        this.name = name;
        this.scripIds = new ArrayList<>();
    }

    public Group(String name, List<String> scripIds) {
        validateName(name);
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
            throw new IllegalArgumentException("Group name must be between 1 and 255 characters");
        }
    }

}
