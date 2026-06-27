package com.whiteowl.core.ranker;

import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.ranker.ranker.AccumulationRanker;
import com.whiteowl.core.ranker.ranker.GainersRanker;
import com.whiteowl.core.ranker.ranker.RatioRanker;
import com.whiteowl.core.ranker.ranker.StandardDeviationRanker;

import java.util.List;

public final class RankerRegistry {

    private final List<Ranker> rankers;

    public RankerRegistry(BarsRepository barsRepository) {
        this.rankers = List.of(
                new AccumulationRanker(),
                new GainersRanker(),
                new StandardDeviationRanker(),
                new RatioRanker(barsRepository)
        );
    }

    public List<Ranker> getRankers() {
        return rankers;
    }

}
