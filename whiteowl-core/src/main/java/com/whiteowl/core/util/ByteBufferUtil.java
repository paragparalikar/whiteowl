package com.whiteowl.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@UtilityClass
public final class ByteBufferUtil {

    public static ByteBuffer allocateLittleEndian(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    public static ByteBuffer wrapLittleEndian(byte[] array) {
        return ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN);
    }

}
