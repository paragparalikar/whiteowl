package com.whiteowl.scripting.ranker;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.scripting.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.scripting.ranker.patternmatch.PatternMatchRanker;
import com.whiteowl.scripting.ranker.script.GroovyRanker;
import com.whiteowl.scripting.script.ScriptDescriptor;
import com.whiteowl.scripting.script.ScriptRepository;

import java.util.ArrayList;
import java.util.List;

public final class RankerRegistry {

    private final ScriptRepository rankerScriptRepository;
    private final BarsRepository barsRepository;
    private final ExampleGroupRepository exampleGroupRepository;

    public RankerRegistry(ScriptRepository rankerScriptRepository,
                          BarsRepository barsRepository,
                          ExampleGroupRepository exampleGroupRepository) {
        this.rankerScriptRepository = rankerScriptRepository;
        this.barsRepository = barsRepository;
        this.exampleGroupRepository = exampleGroupRepository;
    }

    public List<Ranker> getRankers() {
        List<Ranker> all = new ArrayList<>();
        all.add(new PatternMatchRanker(barsRepository, exampleGroupRepository));
        for (ScriptDescriptor descriptor : rankerScriptRepository.findAll()) {
            all.add(new GroovyRanker(descriptor, rankerScriptRepository));
        }
        return all;
    }

}
