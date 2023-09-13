package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.KiteDepth;
import com.whiteowl.core.quote.Depth;

public class DepthMapper {

	public Depth toDepth(KiteDepth kiteDepth) {
		if(null == kiteDepth) return null;
		final Depth depth = new Depth();
		depth.setOrders(kiteDepth.getOrders());
		depth.setPrice(kiteDepth.getPrice());
		depth.setQuantity(kiteDepth.getQuantity());
		return depth;
	}
	
}
