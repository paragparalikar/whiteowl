package com.whiteowl.client.kite.adapter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;

import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.client.kite.model.Candle;
import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.core.bar.BarDataProvider;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.query.BarQuery;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteDataProvider;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KiteBarDataProvider implements BarDataProvider, QuoteDataProvider {
	
	private final KiteMapper kiteMapper;
	private final KiteConnectApi kiteClient;
	private final KiteInstrumentService kiteInstrumentService;
	
	public KiteBarDataProvider(KiteMapper kiteMapper, KiteClientProvider kiteClientProvider,
			KiteInstrumentService kiteInstrumentService, KiteDataProviderCredentials kiteDataProviderCredentials) {
		super();
		this.kiteMapper = kiteMapper;
		this.kiteInstrumentService = kiteInstrumentService;
		this.kiteClient = kiteClientProvider.getClient(kiteDataProviderCredentials.toKiteCredentials());
	}
	
	@Override
	public List<Bar> getBars(@NonNull final BarQuery barQuery) {
		final Scrip scrip = barQuery.getScrip();
		final Timeframe timeframe = barQuery.getTimeframe();
		ZonedDateTime to = barQuery.getTo();
		ZonedDateTime from = barQuery.getFrom();
		final Instrument instrument = kiteInstrumentService.findByTradingSymbol(scrip.getCode());
		final long instrumentToken = instrument.getInstrumentToken();
		final Duration duration = kiteMapper.getHistoricalDataBatchLimit(timeframe);
		final List<Bar> bars = new ArrayList<>();
		while(null != to && to.isAfter(from)) {
			final ZonedDateTime projectedFrom = to.minus(duration);
			final ZonedDateTime effectiveFrom = from.isAfter(projectedFrom) ? from : projectedFrom;
			to = fill(instrumentToken, scrip.getCode(), timeframe, effectiveFrom, to, bars);
		}
		if(log.isDebugEnabled()) log.debug("Downloaded total {} bars from kite for Code : {}, Timeframe : {}", bars.size(), scrip.getCode(), timeframe);
		return bars;
	}
	
	private ZonedDateTime fill(long instrumentToken, String code, Timeframe timeframe, 
			ZonedDateTime from, ZonedDateTime to, List<Bar> bars){
		final String interval = kiteMapper.toInterval(timeframe);
		final CandleSeries candleSeries = kiteClient.getData(instrumentToken, interval, from, to);
		if(null != candleSeries && null != candleSeries.getData() && !candleSeries.getData().isEmpty()) {
			final List<Candle> candles = candleSeries.getData();
			candles.stream()
				.map(candle -> kiteMapper.toBar(candle, timeframe))
				.forEach(bars::add);
			return candles.get(0).getTimestamp().minus(timeframe.getDuration());
		}
		return null;
	}

	@Override
	public Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode) {
		final KiteQuoteMode kiteQuoteMode = kiteMapper.toKiteQuoteMode(mode);
		final Set<Instrument> instruments = scrips.stream()
			.map(Scrip::getCode)
			.map(kiteInstrumentService::findByTradingSymbol)
			.collect(Collectors.toSet());
		final Collection<KiteQuote> kiteQuotes = kiteClient.getQuotes(instruments, kiteQuoteMode);
		return kiteQuotes.stream()
				.map(kiteMapper::toQuote)
				.collect(Collectors.toSet());
	}

}
