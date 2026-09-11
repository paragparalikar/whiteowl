package com.whiteowl.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathResolver {

    private static final String DATA_DIR = "data";
    private static final String BARS_DIR = "bars";
    private static final String BIN_EXTENSION = ".bin";

    public static Path resolveBarsFile(Path baseDir, String scripId, String timeframeLabel) {
        return baseDir.resolve(DATA_DIR)
                .resolve(BARS_DIR)
                .resolve(scripId)
                .resolve(timeframeLabel + BIN_EXTENSION);
    }

}
