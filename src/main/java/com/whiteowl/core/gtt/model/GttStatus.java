package com.whiteowl.core.gtt.model;

public enum GttStatus {

    ACTIVE(false),
    TRIGGERED(true),
    DISABLED(true),
    EXPIRED(true),
    CANCELLED(true),
    REJECTED(true);

    private final boolean terminal;

    GttStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }

}
