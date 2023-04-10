package com.whiteowl.core.derivative.option.chain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;

@Order
@Component
@RequiredArgsConstructor
public class OptionChainBuilderCommandLineRunner implements CommandLineRunner {

	private final BarService barService;
	private final ScripService scripService;
	private final OptionChainBuilder optionChainBuilder;
	
	@Override
	public void run(String... args) throws Exception {
		final LocalDate expiry = LocalDate.of(2023, 04, 6);
		final ZonedDateTime startTimestamp = ZonedDateTime.of(LocalDate.of(2023, 4, 3), LocalTime.of(9, 15), ZoneId.systemDefault());
		final ZonedDateTime endTimestamp = ZonedDateTime.of(LocalDate.of(2023, 04, 6), LocalTime.of(3, 30), ZoneId.systemDefault());
		final Scrip scrip = scripService.findByCode(Index.NIFTY50.getCode());
		final List<Bar> bars = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(scrip.getCode(), 
				Timeframe.M3, 5 * Timeframe.M3.getDayMultiple()).getBarData().stream()
				.filter(bar -> !bar.getBeginTime().isBefore(startTimestamp))
				.filter(bar -> !bar.getBeginTime().isAfter(endTimestamp))
				.collect(Collectors.toList());
		final int count = 20;
		final List<String> lines = new ArrayList<>();
		lines.add(header(count));
		for(int index = 0; index < bars.size() - count; index++) {
			final Bar bar = bars.get(index);
			final double vix = barService.findByCodeAndTimeframeAndBeginTime(
					Index.VIX.getCode(), Timeframe.M3, bar.getBeginTime())
					.getClosePrice().doubleValue() / Math.sqrt(8760);
			final OptionChain optionChain = optionChainBuilder.build(scrip, expiry, bar);
			final OptionChainReport report = new OptionChainReport(optionChain);
			final String csv = line(report, bars.subList(index, index + count), vix);
			System.out.println(csv);
			lines.add(csv);
		}
		Files.write(Paths.get("C:/trading/data", scrip.getCode() + ".csv"), lines);
	}
	
	private String header(int count) {
		return String.join(",", "pcrOi", "pcrVolume", "pcrPrice", 
				"pcrOtmOi", "pcrOtmVolume", "pcrOtmPrice", 
				"callOiRatio", "callVolumeRatio", "putOiRatio", "putVolumeRatio",
				"hourlyVix", "spot-20");
	}
	
	private String line(OptionChainReport report, List<Bar> bars, double vix) {
		final double value = bars.get(0).getClosePrice().doubleValue();
		return String.join(",", 
				String.valueOf(report.getPcrOi()), String.valueOf(report.getPcrVolume()), String.valueOf(report.getPcrPrice()),
				String.valueOf(report.getPcrOtmOi()), String.valueOf(report.getPcrOtmVolume()), String.valueOf(report.getPcrOtmVolume()),
				String.valueOf(report.getCallOiRatio()), String.valueOf(report.getCallVolumeRatio()), String.valueOf(report.getPutOiRatio()), 
				String.valueOf(report.getPutVolumeRatio()), String.valueOf(vix),
				String.valueOf((bars.get(19).getClosePrice().doubleValue() - value)*100/value));
	}
	
}
