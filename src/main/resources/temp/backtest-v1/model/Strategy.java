package com.whiteowl.core.backtest.model;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public final class Strategy {

    private final String id;
    private final String name;
    private final Path scriptPath;

    @Override
    public String toString() {
        return name;
    }

}
