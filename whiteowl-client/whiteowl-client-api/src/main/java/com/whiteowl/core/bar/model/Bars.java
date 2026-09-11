package com.whiteowl.core.bar.model;

public class Bars {

    private static final int DEFAULT_CAPACITY = 1024;

    private final String scripId;
    private final Timeframe timeframe;
    private float[] open;
    private float[] high;
    private float[] low;
    private float[] close;
    private long[] volume;
    private long[] timestamp;
    private int size;

    public Bars(String scripId, Timeframe timeframe) {
        this(scripId, timeframe, DEFAULT_CAPACITY);
    }

    public Bars(String scripId, Timeframe timeframe, int capacity) {
        this.scripId = scripId;
        this.timeframe = timeframe;
        this.open = new float[capacity];
        this.high = new float[capacity];
        this.low = new float[capacity];
        this.close = new float[capacity];
        this.volume = new long[capacity];
        this.timestamp = new long[capacity];
        this.size = 0;
    }

    public long getTimestamp(int index) {
        return timestamp[index];
    }

    public float getOpen(int index) {
        return open[index];
    }

    public float getHigh(int index) {
        return high[index];
    }

    public float getLow(int index) {
        return low[index];
    }

    public float getClose(int index) {
        return close[index];
    }

    public long getVolume(int index) {
        return volume[index];
    }

    public final void append(long ts, float o, float h, float l, float c, long vol) {
        ensureCapacity(size + 1);
        timestamp[size] = ts;
        open[size] = o;
        high[size] = h;
        low[size] = l;
        close[size] = c;
        volume[size] = vol;
        size++;
    }

    public final void setBar(int index, long ts, float o, float h, float l, float c, long vol) {
        timestamp[index] = ts;
        open[index] = o;
        high[index] = h;
        low[index] = l;
        close[index] = c;
        volume[index] = vol;
    }

    public final void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public final int capacity() {
        return open.length;
    }

    private void ensureCapacity(int required) {
        if (required <= open.length) {
            return;
        }
        int newCapacity = open.length + (open.length >> 1);
        if (newCapacity < required) {
            newCapacity = required;
        }
        resize(newCapacity);
    }

    private void resize(int newCapacity) {
        open = copyOf(open, newCapacity);
        high = copyOf(high, newCapacity);
        low = copyOf(low, newCapacity);
        close = copyOf(close, newCapacity);
        volume = copyOf(volume, newCapacity);
        timestamp = copyOf(timestamp, newCapacity);
    }

    private static float[] copyOf(float[] src, int newLength) {
        float[] dest = new float[newLength];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }

    private static long[] copyOf(long[] src, int newLength) {
        long[] dest = new long[newLength];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }

    public String getScripId() {
        return this.scripId;
    }

    public Timeframe getTimeframe() {
        return this.timeframe;
    }

    public BarsArrays arrays() {
        return new BarsArrays(open, high, low, close, volume, timestamp, size);
    }

}
