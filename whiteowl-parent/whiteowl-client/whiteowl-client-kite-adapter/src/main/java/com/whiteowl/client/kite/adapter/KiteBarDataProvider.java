package com.whiteowl.client.kite.adapter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;

import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.client.kite.model.Candle;
import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.core.bar.BarDataProvider;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.query.BarQuery;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KiteBarDataProvider implements BarDataProvider {
	
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
		if(null == instrument) return Collections.emptyList();			
		final long instrumentToken = instrument.getInstrumentToken();
		final Duration duration = kiteMapper.getHistoricalDataBatchLimit(timeframe);
		final List<Bar> bars = new ArrayList<>();
		while(null != to && !from.isAfter(to)) {
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

}
