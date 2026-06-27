package com.whiteowl.core.backtest.v2.model;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.ScripType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public final class BacktestConfig {

    public static final float DEFAULT_INITIAL_CAPITAL = 100_000f;
    public static final float DEFAULT_COST_PERCENT = 0.1f;
    public static final float DEFAULT_SLIPPAGE_PERCENT = 0.05f;
    public static final float DEFAULT_VOLUME_PARTICIPATION_PERCENT = 5f;
    public static final int DEFAULT_OFFSET = 0;

    private final ScripType scripType;
    private final Exchange exchange;
    private final Timeframe timeframe;
    private final int offset;
    private final float initialCapital;
    private final float costPercent;
    private final float slippagePercent;
    private final float volumeParticipationPercent;
    @Builder.Default
    private final Map<String, Number> strategyInputs = Map.of();

}
