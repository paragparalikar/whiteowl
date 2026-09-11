package com.whiteowl.core.backtest.v2.smartvalue;

public final class CircularLongBuffer {

    private static final long NAN_SUBSTITUTE = Long.MIN_VALUE;

    private final long[] data;
    private final int capacity;
    private int head;
    private int count;

    public CircularLongBuffer(int capacity) {
        this.capacity = capacity;
        this.data = new long[capacity];
        this.head = 0;
        this.count = 0;
    }

    public void push(long value) {
        data[head] = value;
        head = (head + 1) % capacity;
        if (count < capacity) {
            count++;
        }
    }

    public long get(int offset) {
        if (offset > 0 || -offset >= count) {
            return NAN_SUBSTITUTE;
        }
        int index = ((head - 1 + offset) % capacity + capacity) % capacity;
        return data[index];
    }

    public int size() {
        return count;
    }

    public int capacity() {
        return capacity;
    }

}
