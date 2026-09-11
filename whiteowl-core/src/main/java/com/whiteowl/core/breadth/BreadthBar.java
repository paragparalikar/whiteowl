package com.whiteowl.core.breadth;

/**
 * Single bar of breadth data.
 * @param positive  the positive component (e.g., number of advances, sum of up %)
 * @param negative  the negative component (e.g., number of declines, sum of down %)
 * @param net       positive - negative
 */
public record BreadthBar(float positive, float negative, float net) {}
