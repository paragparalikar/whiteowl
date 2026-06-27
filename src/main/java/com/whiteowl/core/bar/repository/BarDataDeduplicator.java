package com.whiteowl.core.bar.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class BarDataDeduplicator {

    private static final int HEADER_SIZE = 16;
    private static final int BAR_SIZE = 32;
    private static final int MAGIC = 0x42415253;
    private static final short VERSION = 1;
    private static final String FILE_EXTENSION = ".bin";

    public static void main(String[] args) throws IOException {
        Path baseDir = Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + "/.whiteowl"), "data", "bars");
        if (!Files.exists(baseDir)) {
            System.out.println("Base directory not found: " + baseDir);
            return;
        }
        AtomicInteger totalFiles = new AtomicInteger();
        AtomicInteger deduplicatedFiles = new AtomicInteger();
        AtomicInteger totalDuplicates = new AtomicInteger();
        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        totalFiles.incrementAndGet();
                        int removed = deduplicateFile(path);
                        if (removed > 0) {
                            deduplicatedFiles.incrementAndGet();
                            totalDuplicates.addAndGet(removed);
                        }
                    });
        }
        System.out.printf("Scanned %d files, deduplicated %d files, removed %d duplicate bars total%n",
                totalFiles.get(), deduplicatedFiles.get(), totalDuplicates.get());
    }

    private static int deduplicateFile(Path path) {
        try {
            long fileSize = Files.size(path);
            if (fileSize <= HEADER_SIZE) return 0;
            int barCount = (int) ((fileSize - HEADER_SIZE) / BAR_SIZE);
            if (barCount <= 1) return 0;
            ByteBuffer buffer = readFile(path, fileSize);
            Map<Long, byte[]> uniqueBars = new LinkedHashMap<>();
            for (int i = 0; i < barCount; i++) {
                int offset = HEADER_SIZE + i * BAR_SIZE;
                long timestamp = buffer.getLong(offset);
                byte[] barBytes = new byte[BAR_SIZE];
                buffer.position(offset);
                buffer.get(barBytes);
                uniqueBars.put(timestamp, barBytes);
            }
            int duplicates = barCount - uniqueBars.size();
            if (duplicates == 0) return 0;
            int newCount = uniqueBars.size();
            ByteBuffer newBuffer = ByteBuffer.allocate(HEADER_SIZE + newCount * BAR_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN);
            newBuffer.putInt(MAGIC);
            newBuffer.putShort(VERSION);
            newBuffer.putShort((short) 0);
            newBuffer.putLong(newCount);
            for (byte[] barBytes : uniqueBars.values()) {
                newBuffer.put(barBytes);
            }
            newBuffer.flip();
            try (FileChannel channel = FileChannel.open(path,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(newBuffer);
            }
            System.out.printf("%-60s removed %d duplicates (%d -> %d bars)%n",
                    path.getFileName(), duplicates, barCount, newCount);
            return duplicates;
        } catch (IOException e) {
            System.err.println("Error processing " + path + ": " + e.getMessage());
            return 0;
        }
    }

    private static ByteBuffer readFile(Path path, long fileSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.read(buffer);
        }
        buffer.flip();
        return buffer;
    }

}
