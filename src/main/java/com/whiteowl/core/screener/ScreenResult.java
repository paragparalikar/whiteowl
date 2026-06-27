package com.whiteowl.core.screener;

import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ScreenResult {

    private final Scrip scrip;
    private final int totalBars;

}
