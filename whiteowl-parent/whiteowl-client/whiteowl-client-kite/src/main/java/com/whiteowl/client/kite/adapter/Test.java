package com.whiteowl.client.kite.adapter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.whiteowl.core.util.Strings;
import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.client.kite.model.Candle;
import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.InstrumentType;
import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.core.bar.Timeframe;

public class Test {
	
	public static void main(String[] args) throws IOException {
		new Test().download();
	}
	
	private final KiteMapper kiteMapper;
	private final KiteConnectApi kiteClient;
	private final KiteInstrumentService kiteInstrumentService;
	
	private final Path basePath = Paths.get("C://data");
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private Test() {
		final KiteClientProvider kiteClientProvider = new KiteClientProvider();
		final KiteDataProviderCredentials kiteDataProviderCredentials = buildKiteDataProviderCredentials();
		this.kiteClient = kiteClientProvider.getClient(kiteDataProviderCredentials.toKiteCredentials());
		this.kiteInstrumentService = new KiteInstrumentService();
		this.kiteMapper = new KiteMapper(null, kiteInstrumentService);
	}
	
	private KiteDataProviderCredentials buildKiteDataProviderCredentials() {
		System.setProperty("mongoose.crypto.key", "UXytRK5s40Sett3rj+h2dA==");
		final KiteDataProviderCredentials kiteDataProviderCredentials = new KiteDataProviderCredentials();
		kiteDataProviderCredentials.setUsername("Kr8iquq+IQuUtHhn1xMeIQ==");
		kiteDataProviderCredentials.setPassword("StYLdA7W+uC4BRHDt2idyQ==");
		kiteDataProviderCredentials.setPin("excpHxV4+vwDF69SHj3JPA==");
		return kiteDataProviderCredentials;
	}
	
	private void download() throws IOException {
		Files.createDirectories(basePath);
		final ZonedDateTime to = ZonedDateTime.now();
		final ZonedDateTime from = to.minusYears(20);
		for(Instrument instrument : kiteInstrumentService.getAllInstruments()) {
			try {
			final Path path = basePath.resolve(instrument.getTradingsymbol() + ".csv");
			if(KiteExchange.NSE.equals(instrument.getExchange()) && 
					InstrumentType.EQ.equals(instrument.getInstrumentType()) &&
					Strings.hasText(instrument.getName()) &&
					!Files.exists(path)) {
				
					final List<Candle> candles = getCandles(instrument, Timeframe.D, from, to);
					System.out.printf("Fetched %d candles for %s\n", candles.size(), instrument.getTradingsymbol());
					try(final BufferedWriter writer = Files.newBufferedWriter(path)){
						for(Candle candle : candles) {
							writer.write(toCSV(instrument, candle));
							writer.newLine();
						}
					}
				}
			}catch(Exception e) {
				System.err.printf("Failure - %s - %s", instrument.getTradingsymbol(), e.getMessage());
			}
		}
	}
	
	private String toCSV(Instrument instrument, Candle candle) {
		return String.join(",", instrument.getTradingsymbol(), 
				dateTimeFormatter.format(candle.getTimestamp()),
				String.valueOf(candle.getOpen()),
				String.valueOf(candle.getHigh()),
				String.valueOf(candle.getLow()),
				String.valueOf(candle.getClose()),
				String.valueOf(candle.getVolume()));
	}
	
	private List<Candle> getCandles(Instrument instrument, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to) {
		final Duration duration = kiteMapper.getHistoricalDataBatchLimit(timeframe);
		final List<Candle> candles = new ArrayList<>();
		while(null != to && to.isAfter(from)) {
			final ZonedDateTime projectedFrom = to.minus(duration);
			final ZonedDateTime effectiveFrom = from.isAfter(projectedFrom) ? from : projectedFrom;
			to = fill(instrument.getInstrumentToken(), timeframe, effectiveFrom, to, candles);
		}
		return candles;
	}
	
	private ZonedDateTime fill(long instrumentToken, Timeframe timeframe, 
			ZonedDateTime from, ZonedDateTime to, List<Candle> accumulator){
		final String interval = kiteMapper.toInterval(timeframe);
		final CandleSeries candleSeries = kiteClient.getData(instrumentToken, interval, from, to);
		if(null != candleSeries && null != candleSeries.getData() && !candleSeries.getData().isEmpty()) {
			final List<Candle> candles = candleSeries.getData();
			accumulator.addAll(candles);
			return candles.get(0).getTimestamp().minus(timeframe.getDuration());
		}
		return null;
	}
}
