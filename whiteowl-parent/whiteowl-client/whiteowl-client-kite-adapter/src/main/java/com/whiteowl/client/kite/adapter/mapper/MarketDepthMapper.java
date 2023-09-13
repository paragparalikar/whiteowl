package com.whiteowl.client.kite.adapter.mapper;

import java.util.Collections;
import java.util.Optional;

import com.whiteowl.client.kite.model.KiteMarketDepth;
import com.whiteowl.core.quote.MarketDepth;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MarketDepthMapper {

	@NonNull private final DepthMapper depthMapper;
	
	public MarketDepth toMarketDepth(KiteMarketDepth kiteMarketDepth) {
		if(null == kiteMarketDepth) return null;
		
		final MarketDepth marketDepth = new MarketDepth();
		
		Optional.ofNullable(kiteMarketDepth.getBuy()).orElse(Collections.emptyList())
			.stream().map(depthMapper::toDepth)
			.forEach(marketDepth.getBuy()::add);
		
		Optional.ofNullable(kiteMarketDepth.getSell()).orElse(Collections.emptyList())
			.stream().map(depthMapper::toDepth)
			.forEach(marketDepth.getSell()::add);
		
		return marketDepth;
	}
	
}
