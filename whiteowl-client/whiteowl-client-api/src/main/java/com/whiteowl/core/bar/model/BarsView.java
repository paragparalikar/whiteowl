package com.whiteowl.core.bar.model;

public final class BarsView extends Bars {

    private final Bars delegate;
    private int viewSize;

    public BarsView(Bars delegate) {
        super(delegate.getScripId(), delegate.getTimeframe(), 0);
        this.delegate = delegate;
        this.viewSize = delegate.size();
    }

    public void setViewSize(int size) {
        this.viewSize = size;
    }

    @Override
    public int size() {
        return viewSize;
    }

    @Override
    public long getTimestamp(int index) {
        return delegate.getTimestamp(index);
    }

    @Override
    public float getOpen(int index) {
        return delegate.getOpen(index);
    }

    @Override
    public float getHigh(int index) {
        return delegate.getHigh(index);
    }

    @Override
    public float getLow(int index) {
        return delegate.getLow(index);
    }

    @Override
    public float getClose(int index) {
        return delegate.getClose(index);
    }

    @Override
    public long getVolume(int index) {
        return delegate.getVolume(index);
    }

    @Override
    public BarsArrays arrays() {
        BarsArrays full = delegate.arrays();
        return new BarsArrays(full.open(), full.high(), full.low(), full.close(),
                full.volume(), full.timestamp(), viewSize);
    }

}
