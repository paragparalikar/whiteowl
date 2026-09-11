package com.whiteowl.workbench.charting.indicator.volumeprofile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VolumeProfileCountMode {

    CLOSE("Close"),
    RANGE("Range"),
    VOLUME("Volume");

    private final String label;

    @Override
    public String toString() {
        return label;
    }

}
