package com.whiteowl.core.backtest.v2.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeatureCollector implements TradeLifecycleCallback {

    private final List<FeatureDefinition> definitions = new ArrayList<>();
    private final FeatureConsumer consumer;

    public FeatureCollector(FeatureConsumer consumer) {
        this.consumer = consumer;
    }

    public void addDefinition(FeatureDefinition definition) {
        definitions.add(definition);
    }

    public void addDefinitions(List<FeatureDefinition> defs) {
        definitions.addAll(defs);
    }

    public List<FeatureDefinition> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    @Override
    public void onTradeEvent(TradeLifecycleEvent event) {
        FeatureVector vector = buildVector(event);
        consumer.accept(vector);
    }

    public void flush() {
        consumer.flush();
    }

    public void close() {
        consumer.close();
    }

    private FeatureVector buildVector(TradeLifecycleEvent event) {
        FeatureVector vector = new FeatureVector(
                event.getPhase(),
                event.getScripId(),
                event.getPositionId(),
                event.getBarIndex(),
                event.currentTimestamp()
        );
        vector.putFeature("open", event.currentOpen());
        vector.putFeature("high", event.currentHigh());
        vector.putFeature("low", event.currentLow());
        vector.putFeature("close", event.currentClose());
        vector.putFeature("volume", event.currentVolume());
        vector.putFeature("entry_price", event.getEntryPrice());
        vector.putFeature("quantity", event.getQuantity());
        vector.putFeature("bars_in_trade", event.getBarsInTrade());
        vector.putFeature("unrealized_pnl", event.getUnrealizedPnl());
        vector.putFeature("side", event.getSide().ordinal());
        appendTradeRecordFeatures(vector, event);
        appendIndicatorFeatures(vector);
        return vector;
    }

    private void appendTradeRecordFeatures(FeatureVector vector, TradeLifecycleEvent event) {
        if (event.getPhase() != TradeLifecyclePhase.EXIT || event.getTradeRecord() == null) {
            return;
        }
        vector.putFeature("exit_price", event.getTradeRecord().getExitPrice());
        vector.putFeature("gross_pnl", event.getTradeRecord().getGrossPnl());
        vector.putFeature("net_pnl", event.getTradeRecord().getNetPnl());
        vector.putFeature("net_pnl_percent", event.getTradeRecord().getNetPnlPercent());
        vector.putFeature("holding_bars", event.getTradeRecord().getHoldingBars());
    }

    private void appendIndicatorFeatures(FeatureVector vector) {
        for (FeatureDefinition definition : definitions) {
            vector.putFeature(definition.getName(), definition.readValue());
        }
    }

}
