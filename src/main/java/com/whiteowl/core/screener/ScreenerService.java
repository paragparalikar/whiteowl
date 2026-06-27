package com.whiteowl.core.screener;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class ScreenerService {

    private static final int MIN_BARS_REQUIRED = 1;

    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private volatile boolean cancelled;

    public void run(Screen screen, ScripType scripType, Exchange exchange, Timeframe timeframe, int offset, ScreenerListener listener) {
        run(screen, scripType, exchange, timeframe, offset, listener, null);
    }

    public void run(Screen screen, ScripType scripType, Exchange exchange, Timeframe timeframe, int offset,
                    ScreenerListener listener, Set<String> scripIdFilter) {
        cancelled = false;
        List<Scrip> scrips = scripType == null
                ? scripRepository.findAll()
                : scripRepository.findByScripType(scripType);
        if (exchange != null) {
            scrips = scrips.stream().filter(s -> s.getExchange() == exchange).toList();
        }
        if (scripIdFilter != null && !scripIdFilter.isEmpty()) {
            scrips = scrips.stream().filter(s -> scripIdFilter.contains(s.getId())).toList();
        }
        int total = scrips.size();
        int completed = 0;
        for (Scrip scrip : scrips) {
            if (cancelled) break;
            try {
                Bars bars = loadBars(scrip.getId(), timeframe, offset);
                if (bars != null && bars.size() >= MIN_BARS_REQUIRED && screen.matches(scrip, bars)) {
                    listener.onMatch(scrip);
                }
            } catch (Exception e) {
                log.debug("Screen error for {}: {}", scrip.getSymbol(), e.getMessage());
                listener.onError(scrip, e.getMessage());
            }
            completed++;
            listener.onProgress(completed, total);
        }
    }

    private Bars loadBars(String scripId, Timeframe timeframe, int offset) throws IOException {
        if (offset <= 0) {
            return barsRepository.load(scripId, timeframe);
        }
        int totalBars = barsRepository.countBars(scripId, timeframe);
        int effectiveSize = totalBars - offset;
        if (effectiveSize < MIN_BARS_REQUIRED) {
            return null;
        }
        return barsRepository.loadRange(scripId, timeframe, 0, effectiveSize);
    }

    public void cancel() {
        cancelled = true;
    }

}
