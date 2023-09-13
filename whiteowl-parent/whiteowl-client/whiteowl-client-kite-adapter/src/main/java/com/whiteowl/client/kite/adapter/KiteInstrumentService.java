package com.whiteowl.client.kite.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.javalite.http.Get;
import org.springframework.stereotype.Service;

import com.whiteowl.client.kite.KiteConstant;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.core.util.Constant;
import com.whiteowl.core.util.Strings;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class KiteInstrumentService {
	
	private final Map<Long, Instrument> cacheByTokens = new HashMap<>();
	private final Map<String, Instrument> cacheBySymbol = new HashMap<>();
	private final Path path = Constant.HOME.resolve(Paths.get("database", "kite-instruments.csv"));
	
	@PostConstruct
	public void init() throws Exception {
		getAllInstruments().forEach(instrument -> {
			cacheByTokens.put(instrument.getInstrumentToken(), instrument);
			cacheBySymbol.put(instrument.getTradingsymbol(), instrument);
		});
	}
	
	@SneakyThrows
	public List<Instrument> getAllInstruments() {
		if(!Files.exists(path) || isExpired(path)) {
			download(path);
		}
		return Files.lines(path)
				.filter(line -> !line.startsWith("instrument_token"))
				.map(Instrument::new)
				.collect(Collectors.toList());
	}
	
	private boolean isExpired(Path path) throws IOException {
		final Instant midnight = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant();
		return Files.getLastModifiedTime(path).toInstant().isBefore(midnight);
	}
	
	private void download(Path path) throws IOException {
		Files.createDirectories(path.getParent());
		final InputStream content = new Get(KiteConstant.URL_INSTRUMENTS, 
				KiteConstant.TIMEOUT, KiteConstant.TIMEOUT).getInputStream();
		final String text = Strings.toString(content);
		Files.writeString(path, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE, 
				StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	public Instrument findByInstrumentToken(Long instrumentToken) {
		return cacheByTokens.get(instrumentToken);
	}

	public Instrument findByTradingSymbol(String tradingSymbol) {
		return cacheBySymbol.get(tradingSymbol);
	}
}
