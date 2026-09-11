package com.whiteowl.client.kite;

import lombok.Getter;

@Getter
public final class KiteApiException extends RuntimeException {

    private static final String SEPARATOR = " : ";
    private final int statusCode;

    public KiteApiException(int statusCode, String message) {
        super(statusCode + SEPARATOR + message);
        this.statusCode = statusCode;
    }

}
