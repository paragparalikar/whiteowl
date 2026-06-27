package com.whiteowl.core.ranker;

import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RankResult {

    private final Scrip scrip;
    private final Double value;

}
