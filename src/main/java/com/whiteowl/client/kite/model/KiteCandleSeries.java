package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public final class KiteCandleSeries {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final int IDX_TIMESTAMP = 0;
    private static final int IDX_OPEN = 1;
    private static final int IDX_HIGH = 2;
    private static final int IDX_LOW = 3;
    private static final int IDX_CLOSE = 4;
    private static final int IDX_VOLUME = 5;
    private static final int IDX_OI = 6;

    private List<List<Object>> candles;

    public final List<KiteCandle> toCandles() {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }
        List<KiteCandle> result = new ArrayList<>(candles.size());
        for (int i = 0; i < candles.size(); i++) {
            result.add(parseCandle(candles.get(i)));
        }
        return result;
    }

    private static KiteCandle parseCandle(List<Object> row) {
        long ts = ZonedDateTime.parse((String) row.get(IDX_TIMESTAMP), FORMATTER)
                .toInstant().toEpochMilli();
        int oi = row.size() > IDX_OI ? toInt(row.get(IDX_OI)) : 0;
        return KiteCandle.builder()
                .timestamp(ts)
                .open(toFloat(row.get(IDX_OPEN)))
                .high(toFloat(row.get(IDX_HIGH)))
                .low(toFloat(row.get(IDX_LOW)))
                .close(toFloat(row.get(IDX_CLOSE)))
                .volume(toInt(row.get(IDX_VOLUME)))
                .openInterest(oi)
                .build();
    }

    private static float toFloat(Object value) {
        if (value instanceof Float f) return f;
        if (value instanceof Double d) return d.floatValue();
        if (value instanceof Number n) return n.floatValue();
        return 0f;
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

}
