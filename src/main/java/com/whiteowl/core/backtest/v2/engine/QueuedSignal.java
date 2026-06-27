package com.whiteowl.core.backtest.v2.engine;

import static com.whiteowl.core.backtest.v2.engine.FillTiming.CURRENT_BAR;
import static com.whiteowl.core.backtest.v2.engine.FillTiming.NEXT_BAR;

public final class QueuedSignal {

    private final SignalType type;
    private final int quantity;
    private final int positionId;
    private final FillTiming fillTiming;
    private final FillPrice fillPrice;
    private final float capturedOpen;
    private final float capturedHigh;
    private final float capturedLow;
    private final float capturedClose;

    private QueuedSignal(SignalType type, int quantity, int positionId,
                         FillTiming fillTiming, FillPrice fillPrice,
                         float capturedOpen, float capturedHigh,
                         float capturedLow, float capturedClose) {
        this.type = type;
        this.quantity = quantity;
        this.positionId = positionId;
        this.fillTiming = fillTiming;
        this.fillPrice = fillPrice;
        this.capturedOpen = capturedOpen;
        this.capturedHigh = capturedHigh;
        this.capturedLow = capturedLow;
        this.capturedClose = capturedClose;
    }

    public static QueuedSignal entry(SignalType type, int quantity,
                                     FillTiming fillTiming, FillPrice fillPrice,
                                     float open, float high, float low, float close) {
        FillPrice resolvedPrice = fillPrice;
        if (fillTiming == CURRENT_BAR && fillPrice == FillPrice.OPEN) {
            resolvedPrice = FillPrice.CLOSE;
        }
        if (fillTiming == NEXT_BAR && fillPrice == FillPrice.CLOSE) {
            resolvedPrice = FillPrice.OPEN;
        }
        return new QueuedSignal(type, quantity, -1, fillTiming, resolvedPrice,
                open, high, low, close);
    }

    public static QueuedSignal exit(SignalType type, int positionId,
                                    FillTiming fillTiming, FillPrice fillPrice,
                                    float open, float high, float low, float close) {
        FillPrice resolvedPrice = fillPrice;
        if (fillTiming == CURRENT_BAR && fillPrice == FillPrice.OPEN) {
            resolvedPrice = FillPrice.CLOSE;
        }
        if (fillTiming == NEXT_BAR && fillPrice == FillPrice.CLOSE) {
            resolvedPrice = FillPrice.OPEN;
        }
        return new QueuedSignal(type, 0, positionId, fillTiming, resolvedPrice,
                open, high, low, close);
    }

    public float resolvePrice(float open, float high, float low, float close) {
        if (fillTiming == CURRENT_BAR) {
            return fillPrice.resolve(capturedOpen, capturedHigh, capturedLow, capturedClose);
        }
        return fillPrice.resolve(open, high, low, close);
    }

    public SignalType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPositionId() {
        return positionId;
    }

    public FillTiming getFillTiming() {
        return fillTiming;
    }

}
