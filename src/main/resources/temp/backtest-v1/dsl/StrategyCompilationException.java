package com.whiteowl.core.backtest.dsl;

import lombok.Getter;

@Getter
public final class StrategyCompilationException extends Exception {

    private final int line;
    private final String detail;

    public StrategyCompilationException(int line, String detail) {
        super(detail);
        this.line = line;
        this.detail = detail;
    }

}
