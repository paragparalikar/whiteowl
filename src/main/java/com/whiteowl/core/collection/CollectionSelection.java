package com.whiteowl.core.collection;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public final class CollectionSelection {

    private final CollectionType type;
    private final Set<String> selectedNames;

    public CollectionSelection(CollectionType type) {
        this.type = type;
        this.selectedNames = new LinkedHashSet<>();
    }

    public CollectionSelection(CollectionType type, Set<String> selectedNames) {
        this.type = type;
        this.selectedNames = new LinkedHashSet<>(selectedNames);
    }

    public boolean isAll() {
        return type == CollectionType.ALL_SCRIPS || selectedNames.isEmpty();
    }

}
