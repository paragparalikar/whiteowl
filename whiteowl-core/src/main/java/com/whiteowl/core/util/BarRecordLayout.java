package com.whiteowl.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BarRecordLayout {

    public static final int RECORD_SIZE = 40;
    public static final int TIMESTAMP_OFFSET = 0;
    public static final int OPEN_OFFSET = 8;
    public static final int HIGH_OFFSET = 12;
    public static final int LOW_OFFSET = 16;
    public static final int CLOSE_OFFSET = 20;
    public static final int VOLUME_OFFSET = 24;
    public static final int RESERVED_OFFSET = 32;
    public static final int TIMESTAMP_SIZE = 8;
    public static final int FLOAT_SIZE = 4;
    public static final int VOLUME_SIZE = 8;
    public static final int RESERVED_SIZE = 8;
    public static final int MMAP_THRESHOLD = 128 * 1024;

}
