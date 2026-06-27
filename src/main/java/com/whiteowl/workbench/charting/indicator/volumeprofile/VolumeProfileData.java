package com.whiteowl.workbench.charting.indicator.volumeprofile;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public final class VolumeProfileData {

    private final List<VolumeProfileBin> bins;
    private final long maxCount;
    @Setter private int fromBar;
    @Setter private int toBar;
    private double offsetX;

    public VolumeProfileData(List<VolumeProfileBin> bins) {
        this.bins = bins;
        this.maxCount = bins.stream().mapToLong(VolumeProfileBin::getCount).max().orElse(1);
    }

    public VolumeProfileData(List<VolumeProfileBin> bins, int fromBar, int toBar) {
        this(bins);
        this.fromBar = fromBar;
        this.toBar = toBar;
    }

    public void addOffsetX(double delta) {
        this.offsetX += delta;
    }

    public void resetOffsetX() {
        this.offsetX = 0;
    }

}
