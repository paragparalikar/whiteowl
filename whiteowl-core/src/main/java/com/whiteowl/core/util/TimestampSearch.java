package com.whiteowl.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimestampSearch {

    public static final int NOT_FOUND = -1;

    public static int findExact(long[] timestamps, int size, long target) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = timestamps[mid];
            if (midVal < target) {
                low = mid + 1;
            } else if (midVal > target) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return NOT_FOUND;
    }

    public static int findFloor(long[] timestamps, int size, long target) {
        int low = 0;
        int high = size - 1;
        int result = NOT_FOUND;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = timestamps[mid];
            if (midVal <= target) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    public static int findCeiling(long[] timestamps, int size, long target) {
        int low = 0;
        int high = size - 1;
        int result = NOT_FOUND;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = timestamps[mid];
            if (midVal >= target) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}
