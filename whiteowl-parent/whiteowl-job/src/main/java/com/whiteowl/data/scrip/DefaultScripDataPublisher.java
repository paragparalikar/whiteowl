package com.whiteowl.data.scrip;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.attribute.AttributeService;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripDataProvider;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultScripDataPublisher implements ScripDataPublisher {
	private static final String KEY = "mongoose.data.download.scrips.nse.date";
	private final ScripService scripService;
	private final AttributeService attributeService;
	private final ScripDataProvider scripDataProvider;
	private final Set<ScripDataSubscriber> subscribers = Collections.newSetFromMap(new IdentityHashMap<>());
	
	@Scheduled(cron = "0 0 9 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		if(shouldDownload()) {
			log.info("Initiating scrip download");
			final List<Scrip> scrips = scripDataProvider.getAllScrips();
			final List<Scrip> scripsWithIndices = updateIndices(scrips);
			scripService.saveAll(scripsWithIndices);
			log.info("Downloaded {} scrips", scripsWithIndices.size());
			attributeService.set(KEY, LocalDate.now());
			subscribers.forEach(subscriber -> subscriber.onScrips(scripsWithIndices));
		}
	}
	
	@Override
	public void subscribe(ScripDataSubscriber subscriber) {
		subscribers.add(subscriber);
	}
	
	private boolean shouldDownload() {
		final LocalDate now = LocalDate.now();
		final LocalDate lastDownloadDate = attributeService.getLocalDate(KEY);
		return null == lastDownloadDate || 
				(lastDownloadDate.isBefore(now.minusDays(1)) &&
				!DayOfWeek.SATURDAY.equals(now.getDayOfWeek()) &&
				!DayOfWeek.SUNDAY.equals(now.getDayOfWeek()));
	}

	private List<Scrip> updateIndices(List<Scrip> scrips){
		final Map<String, Scrip> scripsByCode = scrips.stream()
				.filter(scrip -> Exchange.NSE.equals(scrip.getExchange()))
				.collect(Collectors.toMap(Scrip::getCode, Function.identity()));
		Arrays.stream(Index.values()).forEach(index -> 
				resolveComponents(index).stream()
						.map(code -> scripsByCode.get(code))
						.filter(Objects::nonNull)
						.forEach(scrip -> scrip.getIndices().add(index)));
		return scrips;
	}

	@SneakyThrows
	private Set<String> resolveComponents(Index index) {
		final InputStream stream = new URL(index.getUrl()).openStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		return reader.lines()
				.skip(1)
				.map(line -> line.split(","))
				.map(tokens -> tokens[2])
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
}
