package com.whiteowl.core.screener.dtw;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.examplegroup.model.Example;
import com.whiteowl.core.examplegroup.model.ExampleGroup;
import com.whiteowl.core.util.TimestampSearch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.whiteowl.core.screener.dtw.DtwCalculator.normalize;
import static com.whiteowl.core.util.TimestampSearch.NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
public final class TemplateExtractor {

    private final BarsRepository barsRepository;

    public List<float[]> extractTemplates(ExampleGroup group, int templateBars) {
        List<float[]> templates = new ArrayList<>();
        for (Example example : group.getExamples()) {
            float[] template = extractSingle(example, templateBars);
            if (template != null) {
                templates.add(template);
            }
        }
        log.info("Extracted {} templates from group '{}'", templates.size(), group.getName());
        return templates;
    }

    private float[] extractSingle(Example example, int templateBars) {
        try {
            Bars bars = barsRepository.load(example.getScripId(), example.getTimeframe());
            if (bars == null || bars.size() < templateBars) return null;
            int markerIndex = findMarkerIndex(bars, example.getTimestamp());
            if (markerIndex == NOT_FOUND) return null;
            int startIndex = markerIndex - templateBars + 1;
            if (startIndex < 0) return null;
            float[] closes = extractCloses(bars, startIndex, templateBars);
            return normalize(closes, 0, closes.length);
        } catch (IOException e) {
            log.debug("Failed to extract template for scrip={}: {}", example.getScripId(), e.getMessage());
            return null;
        }
    }

    private int findMarkerIndex(Bars bars, long timestamp) {
        long[] timestamps = bars.arrays().timestamp();
        return TimestampSearch.findFloor(timestamps, bars.size(), timestamp);
    }

    private float[] extractCloses(Bars bars, int from, int length) {
        float[] closes = new float[length];
        for (int i = 0; i < length; i++) {
            closes[i] = bars.getClose(from + i);
        }
        return closes;
    }

}
