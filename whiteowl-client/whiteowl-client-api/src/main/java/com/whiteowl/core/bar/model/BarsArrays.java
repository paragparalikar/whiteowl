package com.whiteowl.core.bar.model;

public record BarsArrays(float[] open, float[] high, float[] low, float[] close,
                          long[] volume, long[] timestamp, int size) {

}
