package com.whiteowl.core.scrip.repository;

import com.whiteowl.core.scrip.model.Scrip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ScripSearchTrie {

    private final TrieNode root = new TrieNode();

    void insert(Scrip scrip) {
        insertSuffixes(scrip.getSymbol(), scrip);
        insertSuffixes(scrip.getName(), scrip);
    }

    void remove(Scrip scrip) {
        removeSuffixes(scrip.getSymbol(), scrip);
        removeSuffixes(scrip.getName(), scrip);
    }

    void clear() {
        root.children.clear();
        root.scrips.clear();
    }

    List<Scrip> search(String query, int maxResults) {
        if (query == null || query.isEmpty()) return Collections.emptyList();
        String lower = query.toLowerCase();
        TrieNode node = traverseTo(lower);
        if (node == null) return Collections.emptyList();
        Set<Scrip> results = new LinkedHashSet<>();
        collectAll(node, results, maxResults);
        return new ArrayList<>(results);
    }

    boolean matches(Scrip scrip, String query) {
        if (query == null || query.isEmpty()) return true;
        String lower = query.toLowerCase();
        return scrip.getSymbol().toLowerCase().contains(lower)
                || scrip.getName().toLowerCase().contains(lower);
    }

    private TrieNode traverseTo(String lower) {
        TrieNode node = root;
        for (int i = 0; i < lower.length(); i++) {
            node = node.children.get(lower.charAt(i));
            if (node == null) return null;
        }
        return node;
    }

    private void insertSuffixes(String key, Scrip scrip) {
        if (key == null || key.isEmpty()) return;
        String lower = key.toLowerCase();
        for (int start = 0; start < lower.length(); start++) {
            TrieNode node = root;
            for (int i = start; i < lower.length(); i++) {
                node = node.children.computeIfAbsent(lower.charAt(i), c -> new TrieNode());
            }
            node.scrips.add(scrip);
        }
    }

    private void removeSuffixes(String key, Scrip scrip) {
        if (key == null || key.isEmpty()) return;
        String lower = key.toLowerCase();
        for (int start = 0; start < lower.length(); start++) {
            TrieNode node = root;
            for (int i = start; i < lower.length(); i++) {
                node = node.children.get(lower.charAt(i));
                if (node == null) break;
            }
            if (node != null) node.scrips.remove(scrip);
        }
    }

    private static void collectAll(TrieNode node, Set<Scrip> results, int maxResults) {
        for (Scrip scrip : node.scrips) {
            if (results.size() >= maxResults) return;
            results.add(scrip);
        }
        for (TrieNode child : node.children.values()) {
            if (results.size() >= maxResults) return;
            collectAll(child, results, maxResults);
        }
    }

    private static final class TrieNode {

        private final Map<Character, TrieNode> children = new HashMap<>();
        private final Set<Scrip> scrips = new LinkedHashSet<>();

    }

}
