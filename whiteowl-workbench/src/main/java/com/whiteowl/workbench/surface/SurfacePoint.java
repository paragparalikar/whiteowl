package com.whiteowl.workbench.surface;

/**
 * One point on the optimization surface. {@code rowIndex} identifies the CSV
 * row the point represents, {@code x} and {@code y} are the two base-plane
 * axis values and {@code z} is the surface height value.
 */
public record SurfacePoint(int rowIndex, double x, double y, double z) {
}
