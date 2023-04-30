package com.whiteowl.nse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.IndexPopulator;
import com.whiteowl.core.scrip.Scrip;

import lombok.SneakyThrows;

@Component
public class NseIndexPopulator implements IndexPopulator {
	
	private List<String> underlyingCodes;

	@Override
	public void populate(Collection<Scrip> scrips) {
		final Map<String, Scrip> scripsByCode = scrips.stream()
				.filter(scrip -> Exchange.NSE.equals(scrip.getExchange()))
				.map(this::populate)
				.collect(Collectors.toMap(Scrip::getCode, Function.identity()));
		Arrays.stream(Index.values()).forEach(index -> populate(index, scripsByCode));
	}
	
	private void populate(Index index, Map<String, Scrip> scripsByCode) {
		resolveComponents(index).stream()
			.map(code -> scripsByCode.get(code))
			.filter(Objects::nonNull)
			.forEach(scrip -> scrip.getIndices().add(index));
	}
	
	private Scrip populate(Scrip scrip) {
		final String code = scrip.getCode().trim().toUpperCase();
		scrip.setUnderlying(getUnderlyingCodes().contains(code));
		return scrip;
	}
	
	@SneakyThrows
	private synchronized List<String> getUnderlyingCodes(){
		return null != underlyingCodes ? underlyingCodes :
				(underlyingCodes = Files
					.lines(new ClassPathResource("underlying-nse.csv")
					.getFile().toPath())
					.map(String::trim)
					.map(String::toUpperCase)
					.collect(Collectors.toList()));
	}

	@SneakyThrows
	private Set<String> resolveComponents(Index index) {
		final String url = resolveUrl(index);
		if (null == url)
			return Collections.emptySet();
		final InputStream stream = new URL(url).openStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		return reader.lines().skip(1).map(line -> line.split(",")).map(tokens -> tokens[2]).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private String resolveUrl(Index index) {
		switch (index) {
		case NIFTY50:
			return "https://archives.nseindia.com/content/indices/ind_nifty50list.csv";
		case NIFTYNEXT50:
			return "https://archives.nseindia.com/content/indices/ind_niftynext50list.csv";
		case NIFTY100:
			return "https://archives.nseindia.com/content/indices/ind_nifty100list.csv";
		case NIFTY200:
			return "https://archives.nseindia.com/content/indices/ind_nifty200list.csv";
		case NIFTY500:
			return "https://archives.nseindia.com/content/indices/ind_nifty500list.csv";
		default:
			return null;
		}
	}

}
