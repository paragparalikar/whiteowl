package com.whiteowl.core.script;

import java.util.List;

public final class ScriptCycleException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Circular dependency detected: %s";
    private static final String ARROW = " → ";

    public ScriptCycleException(List<String> chain) {
        super(String.format(MESSAGE_FORMAT, String.join(ARROW, chain)));
    }

}
