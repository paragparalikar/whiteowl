package com.whiteowl.core.backtest.v2.engine;

import java.util.ArrayList;
import java.util.List;

public final class SignalQueue {

    private final List<QueuedSignal> exits = new ArrayList<>();
    private final List<QueuedSignal> entries = new ArrayList<>();
    private boolean hasPendingEntry;

    public void queueEntry(QueuedSignal signal) {
        if (hasPendingEntry) {
            return;
        }
        entries.add(signal);
        hasPendingEntry = true;
    }

    public void queueExit(QueuedSignal signal) {
        if (hasPendingEntry) {
            entries.clear();
            hasPendingEntry = false;
        }
        exits.add(signal);
    }

    public List<QueuedSignal> drainExits() {
        List<QueuedSignal> result = new ArrayList<>(exits);
        exits.clear();
        return result;
    }

    public List<QueuedSignal> drainEntries() {
        List<QueuedSignal> result = new ArrayList<>(entries);
        entries.clear();
        hasPendingEntry = false;
        return result;
    }

    public void requeueEntry(QueuedSignal signal) {
        entries.add(signal);
        hasPendingEntry = true;
    }

    public void requeueExit(QueuedSignal signal) {
        exits.add(signal);
    }

    public boolean isEmpty() {
        return exits.isEmpty() && entries.isEmpty();
    }

    public boolean hasPendingEntry() {
        return hasPendingEntry;
    }

}
