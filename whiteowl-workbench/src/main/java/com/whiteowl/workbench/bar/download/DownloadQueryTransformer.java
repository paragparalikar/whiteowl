package com.whiteowl.workbench.bar.download;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

public final class DownloadQueryTransformer {

    public Optional<DownloadQuery> transform(DownloadQuery query) {
        long seconds = query.getTimeframe().getSeconds();
        ZonedDateTime adjustedFrom = MarketHours.adjustFromDate(query.getFrom(), seconds);
        ZonedDateTime adjustedTo = MarketHours.adjustToDate(query.getTo(), seconds);
        DownloadQuery adjusted = query.toBuilder().from(adjustedFrom).to(adjustedTo).build();
        return isValid(adjusted) ? Optional.of(adjusted) : Optional.empty();
    }

    private boolean isValid(DownloadQuery query) {
        Duration querySpan = Duration.between(query.getFrom(), query.getTo());
        if (querySpan.isZero() || querySpan.isNegative()) return false;
        Duration timeframeSpan = Duration.ofSeconds(query.getTimeframe().getSeconds());
        return querySpan.compareTo(timeframeSpan) >= 0;
    }

}
