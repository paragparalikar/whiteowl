package com.whiteowl.core.backtest.v2.engine;

import com.whiteowl.core.backtest.v2.model.TradeRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class PositionTracker {

    private final List<OpenPosition> positions = new ArrayList<>();
    private final String scripId;
    private final float costPercent;
    private final float slippagePercent;
    private int nextId;

    private static final float HUNDRED = 100f;

    public PositionTracker(String scripId, float costPercent, float slippagePercent) {
        this.scripId = scripId;
        this.costPercent = costPercent;
        this.slippagePercent = slippagePercent;
        this.nextId = 1;
    }

    public OpenPosition openPosition(Side side, int quantity, float rawPrice,
                                     long timestamp, int barIndex, long volume,
                                     float volumeParticipationPercent) {
        int actualQty;
        if (volume <= 0) {
            actualQty = quantity;
        } else {
            int volumeLimit = (int) (volume * volumeParticipationPercent / HUNDRED);
            if (volumeLimit <= 0) {
                return null;
            }
            actualQty = Math.min(quantity, volumeLimit);
        }
        if (actualQty <= 0) {
            return null;
        }
        float slippageDir = side == Side.LONG ? 1f : -1f;
        float entryPrice = rawPrice * (1f + slippageDir * slippagePercent / HUNDRED);
        OpenPosition pos = new OpenPosition(nextId++, side, actualQty, entryPrice, timestamp, barIndex);
        positions.add(pos);
        return pos;
    }

    public TradeRecord closePosition(OpenPosition pos, float rawPrice,
                                     long timestamp, int barIndex) {
        float slippageDir = pos.getSide() == Side.LONG ? -1f : 1f;
        float exitPrice = rawPrice * (1f + slippageDir * slippagePercent / HUNDRED);
        positions.remove(pos);
        return buildTradeRecord(pos, exitPrice, timestamp, barIndex);
    }

    public List<TradeRecord> closeAllBySide(Side side, float rawPrice,
                                            long timestamp, int barIndex) {
        List<TradeRecord> trades = new ArrayList<>();
        Iterator<OpenPosition> it = positions.iterator();
        while (it.hasNext()) {
            OpenPosition pos = it.next();
            if (pos.getSide() == side) {
                it.remove();
                float slippageDir = side == Side.LONG ? -1f : 1f;
                float exitPrice = rawPrice * (1f + slippageDir * slippagePercent / HUNDRED);
                trades.add(buildTradeRecord(pos, exitPrice, timestamp, barIndex));
            }
        }
        return trades;
    }

    public List<TradeRecord> closeAll(float rawPrice, long timestamp, int barIndex) {
        List<TradeRecord> trades = new ArrayList<>();
        for (OpenPosition pos : positions) {
            float slippageDir = pos.getSide() == Side.LONG ? -1f : 1f;
            float exitPrice = rawPrice * (1f + slippageDir * slippagePercent / HUNDRED);
            trades.add(buildTradeRecord(pos, exitPrice, timestamp, barIndex));
        }
        positions.clear();
        return trades;
    }

    public OpenPosition findById(int positionId) {
        for (OpenPosition pos : positions) {
            if (pos.getId() == positionId) {
                return pos;
            }
        }
        return null;
    }

    public boolean hasPositions() {
        return !positions.isEmpty();
    }

    public boolean hasPositionsOnSide(Side side) {
        for (OpenPosition pos : positions) {
            if (pos.getSide() == side) {
                return true;
            }
        }
        return false;
    }

    public int totalSize() {
        int total = 0;
        for (OpenPosition pos : positions) {
            total += pos.getQuantity();
        }
        return total;
    }

    public int positionCount() {
        return positions.size();
    }

    public float avgPrice() {
        if (positions.isEmpty()) {
            return 0f;
        }
        float totalCost = 0f;
        int totalQty = 0;
        for (OpenPosition pos : positions) {
            totalCost += pos.getEntryPrice() * pos.getQuantity();
            totalQty += pos.getQuantity();
        }
        return totalCost / totalQty;
    }

    public float unrealizedPnl(float currentPrice) {
        float total = 0f;
        for (OpenPosition pos : positions) {
            total += pos.unrealizedPnl(currentPrice);
        }
        return total;
    }

    public List<OpenPosition> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public int latestEntryBarIndex() {
        int latest = -1;
        for (OpenPosition pos : positions) {
            if (pos.getEntryBarIndex() > latest) {
                latest = pos.getEntryBarIndex();
            }
        }
        return latest;
    }

    private TradeRecord buildTradeRecord(OpenPosition pos, float exitPrice,
                                         long exitTimestamp, int exitBarIndex) {
        float diff = exitPrice - pos.getEntryPrice();
        if (pos.getSide() == Side.SHORT) {
            diff = -diff;
        }
        float grossPnl = diff * pos.getQuantity();
        float entryCost = pos.getEntryPrice() * pos.getQuantity() * costPercent / HUNDRED;
        float exitCost = exitPrice * pos.getQuantity() * costPercent / HUNDRED;
        float totalCost = entryCost + exitCost;
        float netPnl = grossPnl - totalCost;
        float entryValue = pos.getEntryPrice() * pos.getQuantity();
        float netPnlPercent = entryValue > 0 ? netPnl / entryValue * HUNDRED : 0f;
        return TradeRecord.builder()
                .positionId(pos.getId())
                .scripId(scripId)
                .side(pos.getSide())
                .entryPrice(pos.getEntryPrice())
                .exitPrice(exitPrice)
                .entryTimestamp(pos.getEntryTimestamp())
                .exitTimestamp(exitTimestamp)
                .quantity(pos.getQuantity())
                .grossPnl(grossPnl)
                .totalCost(totalCost)
                .netPnl(netPnl)
                .netPnlPercent(netPnlPercent)
                .entryBarIndex(pos.getEntryBarIndex())
                .exitBarIndex(exitBarIndex)
                .holdingBars(exitBarIndex - pos.getEntryBarIndex())
                .build();
    }

}
