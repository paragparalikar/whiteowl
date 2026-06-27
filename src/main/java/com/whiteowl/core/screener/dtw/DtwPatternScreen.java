package com.whiteowl.core.screener.dtw;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.examplegroup.model.ExampleGroup;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.screener.dtw.DtwCalculator.normalize;
import static com.whiteowl.core.screener.dtw.DtwCalculator.subsequenceDtw;

@Slf4j
public final class DtwPatternScreen implements Screen {

    private static final String NAME = "Pattern Match (DTW)";
    private static final String SETTING_EXAMPLE_GROUP = "Example Group";
    private static final String SETTING_TEMPLATE_BARS = "Template Bars";
    private static final String SETTING_CANDIDATE_BARS = "Candidate Bars";
    private static final String SETTING_MAX_DISTANCE = "Max Distance";
    private static final String SETTING_BAND_WIDTH_PCT = "Band Width %";
    private static final String DEFAULT_EXAMPLE_GROUP = "";
    private static final int DEFAULT_TEMPLATE_BARS = 34;
    private static final int DEFAULT_CANDIDATE_BARS = 60;
    private static final double DEFAULT_MAX_DISTANCE = 0.15;
    private static final int DEFAULT_BAND_WIDTH_PCT = 20;

    private final ExampleGroupRepository exampleGroupRepository;
    private final TemplateExtractor templateExtractor;
    private String exampleGroupName = DEFAULT_EXAMPLE_GROUP;
    private int templateBars = DEFAULT_TEMPLATE_BARS;
    private int candidateBars = DEFAULT_CANDIDATE_BARS;
    private double maxDistance = DEFAULT_MAX_DISTANCE;
    private int bandWidthPct = DEFAULT_BAND_WIDTH_PCT;
    private List<float[]> cachedTemplates;

    public DtwPatternScreen(BarsRepository barsRepository, ExampleGroupRepository exampleGroupRepository) {
        this.exampleGroupRepository = exampleGroupRepository;
        this.templateExtractor = new TemplateExtractor(barsRepository);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(
                new ScreenSetting(SETTING_EXAMPLE_GROUP, String.class, DEFAULT_EXAMPLE_GROUP),
                new ScreenSetting(SETTING_TEMPLATE_BARS, Integer.class, DEFAULT_TEMPLATE_BARS),
                new ScreenSetting(SETTING_CANDIDATE_BARS, Integer.class, DEFAULT_CANDIDATE_BARS),
                new ScreenSetting(SETTING_MAX_DISTANCE, Double.class, DEFAULT_MAX_DISTANCE),
                new ScreenSetting(SETTING_BAND_WIDTH_PCT, Integer.class, DEFAULT_BAND_WIDTH_PCT));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SETTING_EXAMPLE_GROUP, exampleGroupName);
        values.put(SETTING_TEMPLATE_BARS, templateBars);
        values.put(SETTING_CANDIDATE_BARS, candidateBars);
        values.put(SETTING_MAX_DISTANCE, maxDistance);
        values.put(SETTING_BAND_WIDTH_PCT, bandWidthPct);
        return values;
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_EXAMPLE_GROUP.equals(name)) {
            exampleGroupName = (String) value;
            cachedTemplates = null;
        } else if (SETTING_TEMPLATE_BARS.equals(name)) {
            templateBars = (int) value;
            cachedTemplates = null;
        } else if (SETTING_CANDIDATE_BARS.equals(name)) {
            candidateBars = (int) value;
        } else if (SETTING_MAX_DISTANCE.equals(name)) {
            maxDistance = (double) value;
        } else if (SETTING_BAND_WIDTH_PCT.equals(name)) {
            bandWidthPct = (int) value;
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        if (exampleGroupName == null || exampleGroupName.isBlank()) return false;
        List<float[]> templates = loadTemplates();
        if (templates.isEmpty()) return false;
        int size = bars.size();
        if (size < candidateBars) return false;
        float[] candidateCloses = extractCloses(bars, size - candidateBars, candidateBars);
        float[] normalizedCandidate = normalize(candidateCloses, 0, candidateCloses.length);
        return matchesAnyTemplate(templates, normalizedCandidate);
    }

    private boolean matchesAnyTemplate(List<float[]> templates, float[] candidate) {
        for (float[] template : templates) {
            double distance = subsequenceDtw(template, candidate, bandWidthPct);
            if (distance <= maxDistance) return true;
        }
        return false;
    }

    private List<float[]> loadTemplates() {
        if (cachedTemplates != null) return cachedTemplates;
        ExampleGroup group = findGroup();
        if (group == null) {
            log.warn("Example group '{}' not found", exampleGroupName);
            cachedTemplates = new ArrayList<>();
            return cachedTemplates;
        }
        cachedTemplates = templateExtractor.extractTemplates(group, templateBars);
        return cachedTemplates;
    }

    private ExampleGroup findGroup() {
        return exampleGroupRepository.loadAll().stream()
                .filter(g -> exampleGroupName.equals(g.getName()))
                .findFirst()
                .orElse(null);
    }

    private float[] extractCloses(Bars bars, int from, int length) {
        float[] closes = new float[length];
        for (int i = 0; i < length; i++) {
            closes[i] = bars.getClose(from + i);
        }
        return closes;
    }

}
