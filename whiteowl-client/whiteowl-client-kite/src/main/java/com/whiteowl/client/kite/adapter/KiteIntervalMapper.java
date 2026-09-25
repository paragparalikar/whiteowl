package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteInterval;
import com.whiteowl.core.bar.model.Timeframe;

final class KiteIntervalMapper {

    public KiteInterval toKiteInterval(Timeframe timeframe) {
        if (timeframe == null) return null;
        return switch (timeframe) {
            case ONE_MINUTE -> KiteInterval.MINUTE;
            case TWO_MINUTE -> null;
            case THREE_MINUTE -> KiteInterval.THREE_MINUTE;
            case FIVE_MINUTE -> KiteInterval.FIVE_MINUTE;
            case TEN_MINUTE -> KiteInterval.TEN_MINUTE;
            case FIFTEEN_MINUTE -> KiteInterval.FIFTEEN_MINUTE;
            case THIRTY_MINUTE -> KiteInterval.THIRTY_MINUTE;
            case ONE_HOUR -> KiteInterval.SIXTY_MINUTE;
            case TWO_HOUR -> KiteInterval.TWO_HOUR;
            case THREE_HOUR -> KiteInterval.THREE_HOUR;
            case DAILY -> KiteInterval.DAY;
            case WEEKLY, MONTHLY -> null;
        };
    }

}
