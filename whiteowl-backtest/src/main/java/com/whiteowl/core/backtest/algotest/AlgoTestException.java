package com.whiteowl.core.backtest.algotest;

public class AlgoTestException extends RuntimeException {
    public AlgoTestException(String message) {
        super(message);
    }

    public AlgoTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
