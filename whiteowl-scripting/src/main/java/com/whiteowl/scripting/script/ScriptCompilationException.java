package com.whiteowl.scripting.script;

import lombok.Getter;

@Getter
public final class ScriptCompilationException extends Exception {

    private final int line;
    private final String detail;

    public ScriptCompilationException(int line, String detail) {
        super(detail);
        this.line = line;
        this.detail = detail;
    }

}
