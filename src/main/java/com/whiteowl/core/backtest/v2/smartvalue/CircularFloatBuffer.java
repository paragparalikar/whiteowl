package com.whiteowl.core.backtest.v2.smartvalue;

public final class CircularFloatBuffer {

    private final float[] data;
    private final int capacity;
    private int head;
    private int count;

    public CircularFloatBuffer(int capacity) {
        this.capacity = capacity;
        this.data = new float[capacity];
        this.head = 0;
        this.count = 0;
    }

    public void push(float value) {
        data[head] = value;
        head = (head + 1) % capacity;
        if (count < capacity) {
            count++;
        }
    }

    public float get(int offset) {
        if (offset > 0 || -offset >= count) {
            return Float.NaN;
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
