package com.whiteowl.core.bar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.util.Constant;

import lombok.NonNull;
import lombok.SneakyThrows;

@Repository
public class FileSystemBarRepository implements BarRepository {
	private static final String NAME = "bars.dat";
	private static final int BYTES = Long.BYTES + 5 * Double.BYTES;
	
	private Path getPath(String code, Timeframe timeframe) {
		return Constant.HOME.resolve(Paths.get(code, timeframe.name(), NAME));
	}
	
	private void write(Bar bar, DataOutput output) throws IOException {
		output.writeLong(bar.getEndTime().toEpochSecond());
		output.writeDouble(bar.getOpenPrice().doubleValue());
		output.writeDouble(bar.getHighPrice().doubleValue());
		output.writeDouble(bar.getLowPrice().doubleValue());
		output.writeDouble(bar.getClosePrice().doubleValue());
		output.writeDouble(bar.getVolume().doubleValue());
	}
	
	private Bar read(Timeframe timeframe, DataInput input) throws IOException {
		final ZoneId zoneId = ZoneId.systemDefault();
		final Instant instant = Instant.ofEpochSecond(input.readLong());
		final ZonedDateTime endTime = ZonedDateTime.ofInstant(instant, zoneId);
		return BaseBar.builder()
				.openPrice(DoubleNum.valueOf(input.readDouble()))
				.highPrice(DoubleNum.valueOf(input.readDouble()))
				.lowPrice(DoubleNum.valueOf(input.readDouble()))
				.closePrice(DoubleNum.valueOf(input.readDouble()))
				.volume(DoubleNum.valueOf(input.readDouble()))
				.timePeriod(timeframe.getDuration())
				.endTime(endTime)
				.build();
	}

	@Override
	@SneakyThrows
	public Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		final Path path = getPath(code, timeframe);
		synchronized(path) {
			if(Files.exists(path)) {
				try(final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")){
					if(file.length() >= BYTES) {
						file.seek(file.length() - BYTES);
						return Optional.of(read(timeframe, file));
					}
				}
			}
		}
		return Optional.empty();
	}

	@Override
	@SneakyThrows
	public List<Bar> findByCodeAndTimeframe(String code, Timeframe timeframe) {
		final Path path = getPath(code, timeframe);
		synchronized(path) {
			if(Files.exists(path)) {
				try(final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")){
					final long length = file.length();
					if(length >= BYTES) {
						final List<Bar> bars = new ArrayList<Bar>((int) (length/BYTES));
						for(long position = 0; position <= length - BYTES; position += BYTES) {
							file.seek(position);
							bars.add(read(timeframe, file));
						}
						return bars;
					}
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	@SneakyThrows
	public List<Bar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count) {
		final Path path = getPath(code, timeframe);
		synchronized(path) {
			if(Files.exists(path)) {
				try(final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")){
					final long length = file.length();
					if(length >= BYTES) {
						final long availableBarCount = (length/BYTES);
						final long effectiveBarCount = Math.min(availableBarCount, count);
						final List<Bar> bars = new ArrayList<Bar>((int) effectiveBarCount);
						for(long position = length - effectiveBarCount * BYTES; position <= length - BYTES; position += BYTES) {
							file.seek(position);
							bars.add(read(timeframe, file));
						}
						return bars;
					}
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	@SneakyThrows
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe) {
		final Path path = getPath(code, timeframe);
		synchronized(path) {
			if(Files.exists(path)) {
				try(final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")){
					final long length = file.length();
					if(length >= BYTES) {
						file.seek(length - BYTES);
						final ZoneId zoneId = ZoneId.systemDefault();
						final Instant instant = Instant.ofEpochSecond(file.readLong());
						final ZonedDateTime endTime = ZonedDateTime.ofInstant(instant, zoneId);
						return Optional.of(endTime.minus(timeframe.getDuration()));
					}
				}
			}
		}
		return Optional.empty();
	}

	@Override
	@SneakyThrows
	public void saveAll(@NonNull final String code, @NonNull final Timeframe timeframe, @NonNull final Collection<Bar> bars) {
		if(null != bars && !bars.isEmpty()) {
			final ZonedDateTime minBeginTime = findMaxBeginTimeByCodeAndTimeframe(code, timeframe)
					.orElse(ZonedDateTime.now().minusYears(100));
			final List<Bar> cleanBars = bars.stream()
					.filter(bar -> bar.getBeginTime().isAfter(minBeginTime) || bar.getBeginTime().equals(minBeginTime))
					.filter(bar -> withinSession(bar.getEndTime()))
					.distinct()
					.sorted(Comparator.comparing(Bar::getEndTime))
					.collect(Collectors.toList());
			if(!cleanBars.isEmpty()) {
				final Path path = getPath(code, timeframe);
				synchronized(path) {
					if(!Files.exists(path)) {
						Files.createDirectories(path.getParent());
						Files.createFile(path);
					}
					try(final RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
						final long startOffset = minBeginTime.equals(cleanBars.get(cleanBars.size() - 1).getBeginTime()) ?
								file.length() - BYTES : file.length();
						file.seek(startOffset);
						for(Bar bar : cleanBars) write(bar, file);
					}
				}
			}
		}
	}
	
	private boolean withinSession(ZonedDateTime zonedDateTime) {
		final LocalTime localTime = zonedDateTime.toLocalTime();
		return Constant.NSE_START_TIME.isBefore(localTime) &&
				(Constant.NSE_END_TIME.isAfter(localTime) || 
						Constant.NSE_END_TIME.equals(localTime));
	}

}