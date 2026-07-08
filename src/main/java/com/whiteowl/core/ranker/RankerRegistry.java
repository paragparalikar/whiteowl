package com.whiteowl.core.ranker;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.ranker.patternmatch.PatternMatchRanker;
import com.whiteowl.core.ranker.script.GroovyRanker;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;

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
