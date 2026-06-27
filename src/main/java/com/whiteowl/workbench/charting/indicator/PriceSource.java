package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceSource {

    CLOSE("Close") {
        @Override
        public float resolve(Bars bars, int index) {
            return bars.getClose(index);
        }
    },
    OPEN("Open") {
        @Override
        public float resolve(Bars bars, int index) {
            return bars.getOpen(index);
        }
    },
    HIGH("High") {
        @Override
        public float resolve(Bars bars, int index) {
            return bars.getHigh(index);
        }
    },
    LOW("Low") {
        @Override
        public float resolve(Bars bars, int index) {
            return bars.getLow(index);
        }
    },
    MID("Mid") {
        @Override
        public float resolve(Bars bars, int index) {
            return (bars.getHigh(index) + bars.getLow(index)) / 2f;
        }
    },
    HLC3("HLC/3") {
        @Override
        public float resolve(Bars bars, int index) {
            return (bars.getHigh(index) + bars.getLow(index) + bars.getClose(index)) / 3f;
        }
    },
    VOLUME("Volume") {
        @Override
        public float resolve(Bars bars, int index) {
            return bars.getVolume(index);
        }
    };

    private final String label;

    public abstract float resolve(Bars bars, int index);

    @Override
    public String toString() {
        return label;
    }

}
