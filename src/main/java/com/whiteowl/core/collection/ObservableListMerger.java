package com.whiteowl.core.collection;

import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ObservableListMerger {

    private ObservableListMerger() {
    }

    public static <T, K> void merge(ObservableList<T> existing, List<T> incoming,
                                    Function<T, K> keyExtractor) {
        Map<K, T> incomingByKey = new LinkedHashMap<>();
        for (T item : incoming) {
            incomingByKey.put(keyExtractor.apply(item), item);
        }
        existing.removeIf(item -> !incomingByKey.containsKey(keyExtractor.apply(item)));
        for (int i = 0; i < existing.size(); i++) {
            K key = keyExtractor.apply(existing.get(i));
            T fresh = incomingByKey.get(key);
            if (fresh != null) {
                existing.set(i, fresh);
            }
        }
        Map<K, Boolean> existingKeys = new LinkedHashMap<>();
        for (T item : existing) {
            existingKeys.put(keyExtractor.apply(item), Boolean.TRUE);
        }
        for (T item : incoming) {
            K key = keyExtractor.apply(item);
            if (!existingKeys.containsKey(key)) {
                existing.add(item);
            }
        }
    }

}
