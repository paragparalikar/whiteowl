package com.whiteowl.ml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import lombok.SneakyThrows;

public class Test {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	public static void main(String[] args) {
		
	}
	
	@SneakyThrows
	private BarSeries getBars(String scripCode) {
		final Path path = Paths.get("C:\\Users\\parag\\Documents\\finance\\trading\\ml\\data-small", scripCode + ".csv");
		final List<Bar> bars = Files.lines(path)
			.map(Test::parse)
			.distinct()
			.sorted(Comparator.comparing(Bar::getEndTime))
			.collect(Collectors.toList());
		return new BaseBarSeries(bars);
	}
	
	private static Bar parse(String line) {
		final String[] tokens = line.split(",");
		return BaseBar.builder()
				.timePeriod(Duration.ofDays(1))
				.endTime(LocalDate.parse(tokens[1], formatter).atStartOfDay(ZoneId.systemDefault()))
				.openPrice(DoubleNum.valueOf(tokens[2]))
				.highPrice(DoubleNum.valueOf(tokens[3]))
				.lowPrice(DoubleNum.valueOf(tokens[4]))
				.closePrice(DoubleNum.valueOf(tokens[5]))
				.volume(DoubleNum.valueOf(tokens[6]))
				.build();
	}
	
}
