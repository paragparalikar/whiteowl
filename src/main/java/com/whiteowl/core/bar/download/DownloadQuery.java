package com.whiteowl.core.bar.download;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder(toBuilder = true)
public final class DownloadQuery {

    private final Scrip scrip;
    private final Timeframe timeframe;
    private final ZonedDateTime from;
    private final ZonedDateTime to;

}
