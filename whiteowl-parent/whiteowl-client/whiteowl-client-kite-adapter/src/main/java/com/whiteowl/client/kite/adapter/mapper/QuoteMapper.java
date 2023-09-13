package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.adapter.KiteInstrumentService;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.core.quote.Ohlc;
import com.whiteowl.core.quote.Quote;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuoteMapper {

	@NonNull private final OhlcMapper ohlcMapper;
	@NonNull private final MarketDepthMapper marketDepthMapper;
	@NonNull private final KiteInstrumentService kiteInstrumentService;
	
	public Quote toQuote(KiteTick tick) {
		final Quote quote = new Quote();
		final Instrument instrument = kiteInstrumentService.findByInstrumentToken(tick.getToken());
		quote.setCode(instrument.getTradingsymbol());
		quote.setLastPrice(tick.getLastTradedPrice());
		final Ohlc ohlc = new Ohlc();
		ohlc.setOpen(tick.getOpenPrice());
		ohlc.setHigh(tick.getHighPrice());
		ohlc.setLow(tick.getLowPrice());
		ohlc.setClose(tick.getClosePrice());
		quote.setOhlc(ohlc);
		quote.setNetChange(tick.getChange());
		quote.setLastQuantity((int)tick.getLastTradedQuantity());
		quote.setAveragePrice(tick.getAverageTradePrice());
		quote.setVolume(tick.getVolumeTradedToday());
		quote.setBuyQuantity((int)tick.getTotalBuyQuantity());
		quote.setSellQuantity((int)tick.getTotalSellQuantity());
		quote.setLastTradeTime(tick.getLastTradedTime());
		quote.setOi(tick.getOi());
		quote.setOiDayHigh(tick.getOiDayHigh());
		quote.setOiDayLow(tick.getOiDayLow());
		quote.setTimestamp(tick.getTickTimestamp());
		quote.setDepth(marketDepthMapper.toMarketDepth(tick.getDepth()));
		return quote;
	}
	
	public Quote toQuote(KiteQuote kiteQuote) {
		final Quote quote = new Quote();
		final Instrument instrument = kiteInstrumentService.findByInstrumentToken(kiteQuote.getInstrumentToken());
		quote.setCode(instrument.getTradingsymbol());
		quote.setTimestamp(kiteQuote.getTimestamp());
		quote.setLastTradeTime(kiteQuote.getLastTradeTime());
		quote.setLastPrice(kiteQuote.getLastPrice());
		quote.setLastQuantity(kiteQuote.getLastQuantity());
		quote.setBuyQuantity(kiteQuote.getBuyQuantity());
		quote.setSellQuantity(kiteQuote.getSellQuantity());
		quote.setVolume(kiteQuote.getVolume());
		quote.setAveragePrice(kiteQuote.getAveragePrice());
		quote.setOi(kiteQuote.getOi());
		quote.setOiDayHigh(kiteQuote.getOiDayHigh());
		quote.setOiDayLow(kiteQuote.getOiDayLow());
		quote.setOhlc(ohlcMapper.toOhlc(kiteQuote.getOhlc()));
		quote.setNetChange(kiteQuote.getNetChange());
		quote.setLowerCircuitLimit(kiteQuote.getLowerCircuitLimit());
		quote.setUpperCircuitLimit(kiteQuote.getUpperCircuitLimit());
		quote.setDepth(marketDepthMapper.toMarketDepth(kiteQuote.getDepth()));
		return quote;
	}
	
}
