package com.whiteowl.client.kite.adapter.mapper;

import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.adapter.KiteInstrumentService;
import com.whiteowl.core.trade.TradeService;

import lombok.experimental.Delegate;

@Component
public class KiteMapper {

	@Delegate private final BarMapper barMapper = new BarMapper();
	@Delegate private final ExchangeMapper exchangeMapper = new ExchangeMapper();
	@Delegate private final ScripTypeMapper scripTypeMapper = new ScripTypeMapper();
	@Delegate private final ScripMapper scripMapper = new ScripMapper(exchangeMapper, scripTypeMapper);
	@Delegate private final TimeframeMapper timeframeMapper = new TimeframeMapper();
	@Delegate private final TradeLimitTypeMapper tradeLimitTypeMapper = new TradeLimitTypeMapper();
	@Delegate private final TradeStatusMapper tradeStatusMapper = new TradeStatusMapper();
	@Delegate private final TradeTypeMapper tradeTypeMapper = new TradeTypeMapper();
	@Delegate private final TradeValidityMapper tradeValidityMapper = new TradeValidityMapper();
	@Delegate private final TradeVarietyMapper tradeVarietyMapper = new TradeVarietyMapper();
	@Delegate private final TradeProductMapper tradeProductMapper = new TradeProductMapper();
	@Delegate private final CredentialsMapper credentialsMapper = new CredentialsMapper();
	@Delegate private final OhlcMapper ohlcMapper = new OhlcMapper();
	@Delegate private final DepthMapper depthMapper = new DepthMapper();
	@Delegate private final QuoteModeMapper quoteModeMapper = new QuoteModeMapper();
	@Delegate private final MarketDepthMapper marketDepthMapper = new MarketDepthMapper(depthMapper);
	@Delegate private final TradeMapper tradeMapper;
	@Delegate private final QuoteMapper quoteMapper;
	
	public KiteMapper(TradeService tradeService, KiteInstrumentService kiteInstrumentService) {
		this.tradeMapper = new TradeMapper(this, tradeService);
		this.quoteMapper = new QuoteMapper(ohlcMapper, marketDepthMapper, kiteInstrumentService);
	}
	
}
