package com.whiteowl.core.screener;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.screener.dtw.DtwPatternScreen;
import com.whiteowl.core.screener.screen.BullFlagScreen;
import com.whiteowl.core.screener.screen.ConsolidationBreakoutScreen;
import com.whiteowl.core.screener.screen.NarrowRangeDayScreen;
import com.whiteowl.core.screener.screen.OversoldReversalScreen;
import com.whiteowl.core.screener.screen.PriceAbove200SmaScreen;
import com.whiteowl.core.screener.screen.RsiOversoldScreen;
import com.whiteowl.core.screener.screen.PivotBreakoutScreen;
import com.whiteowl.core.screener.screen.RegressionBreakoutScreen;
import com.whiteowl.core.screener.screen.ResistanceBreakoutScreen;
import com.whiteowl.core.screener.screen.SimpleBreakoutScreen;
import com.whiteowl.core.screener.screen.TraditionalBreakoutScreen;
import com.whiteowl.core.screener.screen.VolatilityContractionScreen;
import com.whiteowl.core.screener.screen.LiquidityScreen;
import com.whiteowl.core.screener.screen.VolumeSpikeScreen;
import com.whiteowl.core.screener.script.GroovyScreen;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;

import java.util.ArrayList;
import java.util.List;

public final class ScreenRegistry {

    private final List<Screen> builtInScreens;
    private final ScriptRepository screenerScriptRepository;

    public ScreenRegistry(BarsRepository barsRepository, ExampleGroupRepository exampleGroupRepository,
                          ScriptRepository screenerScriptRepository) {
        this.screenerScriptRepository = screenerScriptRepository;
        this.builtInScreens = List.of(
                new PriceAbove200SmaScreen(),
                new RsiOversoldScreen(),
                new VolumeSpikeScreen(),
                new NarrowRangeDayScreen(),
                new PivotBreakoutScreen(),
                new RegressionBreakoutScreen(),
                new SimpleBreakoutScreen(),
                new TraditionalBreakoutScreen(),
                new OversoldReversalScreen(),
                new BullFlagScreen(),
                new VolatilityContractionScreen(),
                new LiquidityScreen(),
                new ResistanceBreakoutScreen(),
                new ConsolidationBreakoutScreen(),
                new DtwPatternScreen(barsRepository, exampleGroupRepository));
    }

    public List<Screen> getScreens() {
        List<Screen> all = new ArrayList<>(builtInScreens);
        for (ScriptDescriptor descriptor : screenerScriptRepository.findAll()) {
            all.add(new GroovyScreen(descriptor, screenerScriptRepository));
        }
        return all;
    }

    public List<Screen> getBuiltInScreens() {
        return builtInScreens;
    }

}
