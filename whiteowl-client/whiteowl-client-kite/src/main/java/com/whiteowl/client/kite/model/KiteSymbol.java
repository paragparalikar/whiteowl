package com.whiteowl.client.kite.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = {"exchange", "exchangeToken"})
public final class KiteSymbol {

    private int instrumentToken;
    private int exchangeToken;
    private String tradingsymbol;
    private String name;
    private float lastPrice;
    private String expiry;
    private float strike;
    private float tickSize;
    private int lotSize;
    private String type;
    private String segment;
    private KiteExchange exchange;

    private static final int TOKEN_INSTRUMENT = 0;
    private static final int TOKEN_EXCHANGE = 1;
    private static final int TOKEN_TRADING_SYMBOL = 2;
    private static final int TOKEN_NAME = 3;
    private static final int TOKEN_LAST_PRICE = 4;
    private static final int TOKEN_EXPIRY = 5;
    private static final int TOKEN_STRIKE = 6;
    private static final int TOKEN_TICK_SIZE = 7;
    private static final int TOKEN_LOT_SIZE = 8;
    private static final int TOKEN_TYPE = 9;
    private static final int TOKEN_SEGMENT = 10;
    private static final int TOKEN_EXCHANGE_NAME = 11;

    public static KiteSymbol parseCsv(String line) {
        String[] tokens = line.split(",");
        return KiteSymbol.builder()
                .instrumentToken(Integer.parseInt(tokens[TOKEN_INSTRUMENT]))
                .exchangeToken(Integer.parseInt(tokens[TOKEN_EXCHANGE]))
                .tradingsymbol(tokens[TOKEN_TRADING_SYMBOL])
                .name(unquote(tokens[TOKEN_NAME]))
                .lastPrice(parseFloatSafe(tokens[TOKEN_LAST_PRICE]))
                .expiry(tokens[TOKEN_EXPIRY])
                .strike(parseFloatSafe(tokens[TOKEN_STRIKE]))
                .tickSize(parseFloatSafe(tokens[TOKEN_TICK_SIZE]))
                .lotSize(parseIntSafe(tokens[TOKEN_LOT_SIZE]))
                .type(tokens[TOKEN_TYPE])
                .segment(tokens[TOKEN_SEGMENT])
                .exchange(KiteExchange.valueOf(tokens[TOKEN_EXCHANGE_NAME]))
                .build();
    }

    private static String unquote(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.replace("\"", "").trim();
    }

    private static float parseFloatSafe(String value) {
        if (value == null || value.isBlank()) {
            return 0f;
        }
        return Float.parseFloat(value);
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public String toCsv() {
        return String.join(",",
                String.valueOf(instrumentToken),
                String.valueOf(exchangeToken),
                tradingsymbol,
                unquote(name),
                String.valueOf(lastPrice),
                expiry,
                String.valueOf(strike),
                String.valueOf(tickSize),
                String.valueOf(lotSize),
                type,
                segment,
                exchange.name());
    }

}
