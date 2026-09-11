package com.whiteowl.workbench.charting.indicator.volumeprofile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class VolumeProfileBin {

    private final float lowPrice;
    private final float highPrice;
    private final long count;

    public float midPrice() {
        return (lowPrice + highPrice) / 2f;
    }

}
