package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.indicator.script.DisplayMode;
import com.whiteowl.core.indicator.script.GroovyIndicator;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileIndicator;

import java.util.ArrayList;
import java.util.List;

public final class IndicatorRegistry {

    private static final List<OverlayIndicator> BUILT_IN_OVERLAY = List.of(
            new SmaIndicator(),
            new EmaIndicator(),
            new SupertrendIndicator(),
            new BollingerBandsIndicator(),
            new VwapIndicator(),
            new ParabolicSarIndicator(),
            new IchimokuCloudIndicator(),
            new KeltnerChannelsIndicator(),
            new DonchianChannelsIndicator(),
            new VolumeSmaIndicator(),
            new SwingHighLowIndicator(),
            new VolumeProfileIndicator()
    );

    private final List<SubChartIndicator> subChartIndicators;
    private final ScriptRepository indicatorScriptRepository;

    public IndicatorRegistry(BarsRepository barsRepository, ScriptRepository indicatorScriptRepository) {
        this.indicatorScriptRepository = indicatorScriptRepository;
        this.subChartIndicators = List.of(
                new RsiIndicator(),
                new MacdIndicator(),
                new StochasticsIndicator(),
                new StochRsiIndicator(),
                new WilliamsRIndicator(),
                new CciIndicator(),
                new UltimateOscillatorIndicator(),
                new AroonIndicator(),
                new ObvIndicator(),
                new AccumulationDistributionIndicator(),
                new CmfIndicator(),
                new MfiIndicator(),
                new AtrIndicator(),
                new AdxIndicator(),
                new StdDevIndicator(),
                new RocIndicator(),
                new BetaIndicator(barsRepository),
                new RatioIndicator(barsRepository)
        );
    }

    public List<OverlayIndicator> getOverlayIndicators() {
        List<OverlayIndicator> all = new ArrayList<>(BUILT_IN_OVERLAY);
        for (ScriptDescriptor descriptor : indicatorScriptRepository.findAll()) {
            GroovyIndicator gi = new GroovyIndicator(descriptor, indicatorScriptRepository);
            DisplayMode mode = gi.discoverDisplayMode();
            if (mode == DisplayMode.OVERLAY || mode == DisplayMode.VOLUME) {
                all.add(new ScriptedOverlayIndicator(gi, descriptor, indicatorScriptRepository,
                        mode == DisplayMode.VOLUME));
            }
        }
        return all;
    }

    public List<SubChartIndicator> getSubChartIndicators() {
        List<SubChartIndicator> all = new ArrayList<>(subChartIndicators);
        for (ScriptDescriptor descriptor : indicatorScriptRepository.findAll()) {
            GroovyIndicator gi = new GroovyIndicator(descriptor, indicatorScriptRepository);
            DisplayMode mode = gi.discoverDisplayMode();
            if (mode == DisplayMode.SUBCHART) {
                all.add(new ScriptedSubChartIndicator(gi, descriptor, indicatorScriptRepository));
            }
        }
        return all;
    }

}
