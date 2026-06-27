package com.whiteowl.client.kite;

import com.whiteowl.client.kite.model.KiteTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class KiteBinaryParser {

    private static final int SEGMENT_NSE_CD = 3;
    private static final int SEGMENT_BSE_CD = 6;
    private static final int SEGMENT_MASK = 0xFF;
    private static final int DIVISOR_NSE_CD = 10000000;
    private static final int DIVISOR_BSE_CD = 10000;
    private static final int DIVISOR_DEFAULT = 100;
    private static final int PACKET_LTP = 8;
    private static final int PACKET_INDEX_SHORT = 28;
    private static final int PACKET_INDEX_LONG = 32;
    private static final int PACKET_QUOTE = 44;
    private static final int PACKET_FULL = 184;

    List<KiteTick> parseBinary(byte[] binaryPackets) {
        List<byte[]> packets = splitPackets(binaryPackets);
        List<KiteTick> ticks = new ArrayList<>(packets.size());
        for (byte[] bin : packets) {
            int token = readInt(bin, 0);
            int segment = token & SEGMENT_MASK;
            int divisor = resolveDivisor(segment);
            KiteTick tick = parsePacket(bin, token, divisor);
            ticks.add(tick);
        }
        return ticks;
    }

    private KiteTick parsePacket(byte[] bin, int token, int divisor) {
        int length = bin.length;
        if (length == PACKET_LTP) {
            return parseLtp(bin, token, divisor);
        }
        if (length == PACKET_INDEX_SHORT || length == PACKET_INDEX_LONG) {
            return parseIndex(bin, token);
        }
        if (length == PACKET_QUOTE) {
            return parseQuote(bin, token, divisor);
        }
        if (length == PACKET_FULL) {
            KiteTick tick = parseQuote(bin, token, divisor);
            tick.setLastTradedTime(readInt(bin, 44) * 1000L);
            return tick;
        }
        return parseLtp(bin, token, divisor);
    }

    private KiteTick parseLtp(byte[] bin, int token, int divisor) {
        KiteTick tick = new KiteTick();
        tick.setToken(token);
        tick.setLastTradedPrice(readFloat(bin, 4) / divisor);
        tick.setLastTradedTime(System.currentTimeMillis());
        return tick;
    }

    private KiteTick parseIndex(byte[] bin, int token) {
        KiteTick tick = new KiteTick();
        tick.setToken(token);
        tick.setLastTradedPrice(readFloat(bin, 4) / DIVISOR_DEFAULT);
        return tick;
    }

    private KiteTick parseQuote(byte[] bin, int token, int divisor) {
        KiteTick tick = new KiteTick();
        tick.setToken(token);
        tick.setLastTradedPrice(readFloat(bin, 4) / divisor);
        tick.setVolumeTradedToday(readInt(bin, 16));
        tick.setLastTradedTime(System.currentTimeMillis());
        return tick;
    }

    private int resolveDivisor(int segment) {
        if (segment == SEGMENT_NSE_CD) return DIVISOR_NSE_CD;
        if (segment == SEGMENT_BSE_CD) return DIVISOR_BSE_CD;
        return DIVISOR_DEFAULT;
    }

    private List<byte[]> splitPackets(byte[] bin) {
        int packetCount = readShort(bin, 0);
        List<byte[]> packets = new ArrayList<>(packetCount);
        int offset = 2;
        for (int i = 0; i < packetCount; i++) {
            int size = readShort(bin, offset);
            packets.add(Arrays.copyOfRange(bin, offset + 2, offset + 2 + size));
            offset += 2 + size;
        }
        return packets;
    }

    private int readInt(byte[] bin, int offset) {
        return ByteBuffer.wrap(bin, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private float readFloat(byte[] bin, int offset) {
        return ByteBuffer.wrap(bin, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private int readShort(byte[] bin, int offset) {
        return ByteBuffer.wrap(bin, offset, 2).order(ByteOrder.BIG_ENDIAN).getShort();
    }

}
