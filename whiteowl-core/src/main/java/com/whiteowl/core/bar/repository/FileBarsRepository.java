package com.whiteowl.core.bar.repository;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Slf4j
public final class FileBarsRepository implements BarsRepository {

    private static final int HEADER_SIZE = 16;
    private static final int BAR_SIZE = 32;
    private static final int MMAP_THRESHOLD = 128 * 1024;
    private static final int MAGIC = 0x42415253;
    private static final short VERSION = 1;
    private static final int HEADER_BAR_COUNT_OFFSET = 8;
    private static final int BAR_TIMESTAMP_OFFSET = 0;
    private static final int BAR_OPEN_OFFSET = 8;
    private static final int BAR_HIGH_OFFSET = 12;
    private static final int BAR_LOW_OFFSET = 16;
    private static final int BAR_CLOSE_OFFSET = 20;
    private static final int BAR_VOLUME_OFFSET = 24;
    private static final String FILE_EXTENSION = ".bin";

    private final Path baseDir;

    public FileBarsRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", "bars"));
    }

    public FileBarsRepository(Path baseDir) {
        this.baseDir = baseDir;
        log.info("FileBarsRepository initialized with baseDir={}", baseDir);
    }

    @Override
    public Bars load(String scripId, Timeframe timeframe) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        if (!Files.exists(path)) return new Bars(scripId, timeframe, 0);
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE) return new Bars(scripId, timeframe, 0);
        ByteBuffer buffer = readBuffer(path, fileSize);
        int barCount = readBarCount(buffer);
        log.debug("Loaded {} bars for {} {} from {}", barCount, scripId, timeframe.name(), path);
        return transposeToBars(buffer, scripId, timeframe, barCount);
    }

    @Override
    public Bars loadRange(String scripId, Timeframe timeframe, int fromIndex, int toIndex) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        if (!Files.exists(path)) return new Bars(scripId, timeframe, 0);
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE) return new Bars(scripId, timeframe, 0);
        int totalBars = (int) ((fileSize - HEADER_SIZE) / BAR_SIZE);
        int clampedFrom = Math.max(0, Math.min(fromIndex, totalBars));
        int clampedTo = Math.max(clampedFrom, Math.min(toIndex, totalBars));
        int count = clampedTo - clampedFrom;
        if (count == 0) return new Bars(scripId, timeframe, 0);
        long offset = HEADER_SIZE + (long) clampedFrom * BAR_SIZE;
        long length = (long) count * BAR_SIZE;
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) length).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.read(buffer, offset);
        }
        buffer.flip();
        return transposeRangeToBars(buffer, scripId, timeframe, count);
    }

    @Override
    public int countBars(String scripId, Timeframe timeframe) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        if (!Files.exists(path)) return 0;
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE) return 0;
        return (int) ((fileSize - HEADER_SIZE) / BAR_SIZE);
    }

    @Override
    public void save(String scripId, Timeframe timeframe, Bars bars) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        ensureParentExists(path);
        int barCount = bars.size();
        ByteBuffer buffer = allocateBuffer(HEADER_SIZE + (long) barCount * BAR_SIZE);
        writeHeader(buffer, barCount);
        writeBars(buffer, bars, 0, barCount);
        buffer.flip();
        try (FileChannel channel = openForWrite(path)) {
            channel.write(buffer);
        }
        log.debug("Saved {} bars for {} {} to {}", barCount, scripId, timeframe.name(), path);
    }

    @Override
    public void append(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        ensureParentExists(path);
        if (!Files.exists(path)) {
            save(scripId, timeframe, sliceBars(bars, timeframe, fromIndex, toIndex));
            return;
        }
        int safeFrom = skipDuplicatesForAppend(scripId, timeframe, bars, fromIndex, toIndex);
        if (safeFrom >= toIndex) return;
        if (isOverwriteRequired(scripId, timeframe, bars, safeFrom)) {
            overwriteLastBar(path, bars, safeFrom);
            safeFrom++;
        }
        if (safeFrom >= toIndex) return;
        int appendCount = toIndex - safeFrom;
        ByteBuffer barBuffer = allocateBuffer((long) appendCount * BAR_SIZE);
        writeBars(barBuffer, bars, safeFrom, toIndex);
        barBuffer.flip();
        try (FileChannel channel = openForAppend(path)) {
            channel.write(barBuffer);
        }
        updateBarCount(path);
        log.debug("Appended {} bars for {} {} to {}", appendCount, scripId, timeframe.name(), path);
    }

    @Override
    public Optional<Long> findLatestTimestamp(String scripId, Timeframe timeframe) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        if (!Files.exists(path)) return Optional.empty();
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE) return Optional.empty();
        int barCount = (int) ((fileSize - HEADER_SIZE) / BAR_SIZE);
        if (barCount == 0) return Optional.empty();
        long lastBarPosition = HEADER_SIZE + (long) (barCount - 1) * BAR_SIZE;
        ByteBuffer tsBuffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.read(tsBuffer, lastBarPosition + BAR_TIMESTAMP_OFFSET);
        }
        tsBuffer.flip();
        return Optional.of(tsBuffer.getLong());
    }

    @Override
    public Optional<Long> findEarliestTimestamp(String scripId, Timeframe timeframe) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        if (!Files.exists(path)) return Optional.empty();
        long fileSize = Files.size(path);
        if (fileSize <= HEADER_SIZE + BAR_SIZE) return Optional.empty();
        ByteBuffer tsBuffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.read(tsBuffer, HEADER_SIZE + BAR_TIMESTAMP_OFFSET);
        }
        tsBuffer.flip();
        return Optional.of(tsBuffer.getLong());
    }

    @Override
    public void prepend(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        ensureParentExists(path);
        if (!Files.exists(path)) {
            save(scripId, timeframe, sliceBars(bars, timeframe, fromIndex, toIndex));
            return;
        }
        int safeTo = skipDuplicatesForPrepend(scripId, timeframe, bars, fromIndex, toIndex);
        if (safeTo <= fromIndex) return;
        long fileSize = Files.size(path);
        long existingDataSize = fileSize - HEADER_SIZE;
        int prependCount = safeTo - fromIndex;
        long prependSize = (long) prependCount * BAR_SIZE;
        long newFileSize = HEADER_SIZE + prependSize + existingDataSize;
        int totalBars = (int) ((newFileSize - HEADER_SIZE) / BAR_SIZE);
        ByteBuffer existingData = ByteBuffer.allocateDirect((int) existingDataSize);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.read(existingData, HEADER_SIZE);
        }
        existingData.flip();
        ByteBuffer newFile = allocateBuffer(newFileSize);
        writeHeader(newFile, totalBars);
        writeBars(newFile, bars, fromIndex, safeTo);
        newFile.put(existingData);
        newFile.flip();
        try (FileChannel channel = openForWrite(path)) {
            channel.write(newFile);
        }
        log.debug("Prepended {} bars for {} {} to {}", prependCount, scripId, timeframe.name(), path);
    }

    @Override
    public boolean exists(String scripId, Timeframe timeframe) {
        return Files.exists(resolvePath(scripId, timeframe));
    }

    @Override
    public void delete(String scripId, Timeframe timeframe) throws IOException {
        Path path = resolvePath(scripId, timeframe);
        Files.deleteIfExists(path);
        log.debug("Deleted bar file for {} {} at {}", scripId, timeframe.name(), path);
    }

    @Override
    public void rename(String oldScripId, String newScripId) throws IOException {
        Path oldDir = resolveScripDir(oldScripId);
        if (!Files.exists(oldDir)) return;
        Path newDir = resolveScripDir(newScripId);
        if (Files.exists(newDir)) {
            log.warn("Target bar directory already exists for {}, skipping rename from {}", newScripId, oldScripId);
            return;
        }
        ensureParentExists(newDir);
        Files.move(oldDir, newDir);
        log.info("Renamed bar data directory from {} to {}", oldDir, newDir);
    }

    private Path resolveScripDir(String scripId) {
        String sanitized = scripId.replace(':', File.separatorChar);
        return baseDir.resolve(sanitized);
    }

    private int skipDuplicatesForAppend(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex)
            throws IOException {
        Optional<Long> latestTs = findLatestTimestamp(scripId, timeframe);
        if (latestTs.isEmpty()) return fromIndex;
        long cutoff = latestTs.get();
        int index = fromIndex;
        while (index < toIndex && bars.getTimestamp(index) < cutoff) {
            index++;
        }
        return index;
    }

    private boolean isOverwriteRequired(String scripId, Timeframe timeframe, Bars bars, int index) throws IOException {
        Optional<Long> latestTs = findLatestTimestamp(scripId, timeframe);
        return latestTs.isPresent() && bars.getTimestamp(index) == latestTs.get();
    }

    private void overwriteLastBar(Path path, Bars bars, int index) throws IOException {
        long fileSize = Files.size(path);
        long lastBarOffset = fileSize - BAR_SIZE;
        ByteBuffer barBuffer = allocateBuffer(BAR_SIZE);
        writeBars(barBuffer, bars, index, index + 1);
        barBuffer.flip();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.write(barBuffer, lastBarOffset);
        }
    }

    private int skipDuplicatesForPrepend(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex)
            throws IOException {
        Optional<Long> earliestTs = findEarliestTimestamp(scripId, timeframe);
        if (earliestTs.isEmpty()) return toIndex;
        long cutoff = earliestTs.get();
        int index = toIndex;
        while (index > fromIndex && bars.getTimestamp(index - 1) >= cutoff) {
            index--;
        }
        return index;
    }

    private ByteBuffer readBuffer(Path path, long fileSize) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            if (fileSize >= MMAP_THRESHOLD) {
                return channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize).order(ByteOrder.LITTLE_ENDIAN);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) fileSize).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(buffer);
            buffer.flip();
            return buffer;
        }
    }

    private int readBarCount(ByteBuffer buffer) {
        return (int) buffer.getLong(HEADER_BAR_COUNT_OFFSET);
    }

    private Bars transposeToBars(ByteBuffer buffer, String scripId, Timeframe timeframe, int barCount) {
        Bars bars = new Bars(scripId, timeframe, barCount);
        for (int i = 0; i < barCount; i++) {
            int offset = HEADER_SIZE + i * BAR_SIZE;
            bars.append(
                    buffer.getLong(offset + BAR_TIMESTAMP_OFFSET),
                    buffer.getFloat(offset + BAR_OPEN_OFFSET),
                    buffer.getFloat(offset + BAR_HIGH_OFFSET),
                    buffer.getFloat(offset + BAR_LOW_OFFSET),
                    buffer.getFloat(offset + BAR_CLOSE_OFFSET),
                    buffer.getLong(offset + BAR_VOLUME_OFFSET));
        }
        return bars;
    }

    private Bars transposeRangeToBars(ByteBuffer buffer, String scripId, Timeframe timeframe, int barCount) {
        Bars bars = new Bars(scripId, timeframe, barCount);
        for (int i = 0; i < barCount; i++) {
            int offset = i * BAR_SIZE;
            bars.append(
                    buffer.getLong(offset + BAR_TIMESTAMP_OFFSET),
                    buffer.getFloat(offset + BAR_OPEN_OFFSET),
                    buffer.getFloat(offset + BAR_HIGH_OFFSET),
                    buffer.getFloat(offset + BAR_LOW_OFFSET),
                    buffer.getFloat(offset + BAR_CLOSE_OFFSET),
                    buffer.getLong(offset + BAR_VOLUME_OFFSET));
        }
        return bars;
    }

    private void writeHeader(ByteBuffer buffer, int barCount) {
        buffer.putInt(MAGIC);
        buffer.putShort(VERSION);
        buffer.putShort((short) 0);
        buffer.putLong(barCount);
    }

    private void writeBars(ByteBuffer buffer, Bars bars, int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            buffer.putLong(bars.getTimestamp(i));
            buffer.putFloat(bars.getOpen(i));
            buffer.putFloat(bars.getHigh(i));
            buffer.putFloat(bars.getLow(i));
            buffer.putFloat(bars.getClose(i));
            buffer.putLong(bars.getVolume(i));
        }
    }

    private void updateBarCount(Path path) throws IOException {
        long fileSize = Files.size(path);
        int totalBars = (int) ((fileSize - HEADER_SIZE) / BAR_SIZE);
        ByteBuffer header = allocateBuffer(HEADER_SIZE);
        writeHeader(header, totalBars);
        header.flip();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.write(header, 0);
        }
    }

    private Bars sliceBars(Bars source, Timeframe timeframe, int fromIndex, int toIndex) {
        Bars slice = new Bars(source.getScripId(), timeframe, toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            slice.append(source.getTimestamp(i), source.getOpen(i), source.getHigh(i),
                    source.getLow(i), source.getClose(i), source.getVolume(i));
        }
        return slice;
    }

    private FileChannel openForWrite(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private FileChannel openForAppend(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    private ByteBuffer allocateBuffer(long size) {
        return ByteBuffer.allocateDirect((int) size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private Path resolvePath(String scripId, Timeframe timeframe) {
        String sanitized = scripId.replace(':', File.separatorChar);
        return baseDir.resolve(sanitized).resolve(timeframe.getLabel() + FILE_EXTENSION);
    }

    private void ensureParentExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
