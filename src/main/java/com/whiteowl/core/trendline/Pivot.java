package com.whiteowl.core.trendline;

public final class Pivot {

    private final int index;
    private final float price;

    public Pivot(int index, float price) {
        this.index = index;
        this.price = price;
    }

    public int getIndex() {
        return index;
    }

    public float getPrice() {
        return price;
    }

}
