package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.Product;
import com.whiteowl.core.trade.TradeProduct;

public class TradeProductMapper {

	public TradeProduct toTradeProduct(Product product) {
		if(null == product) return null;
		switch(product) {
		case BO:return TradeProduct.BO;
		case CNC:return TradeProduct.CNC;
		case CO:return TradeProduct.CO;
		case MIS:return TradeProduct.MIS;
		case NRML:return TradeProduct.NRML;
		default: throw new IllegalArgumentException(String.format("Product %s is not supported", product.name()));
		}
	}
	
	public Product toProduct(TradeProduct tradeProduct) {
		if(null == tradeProduct) return null;
		switch(tradeProduct) {
		case BO:return Product.BO;
		case CNC:return Product.CNC;
		case CO:return Product.CO;
		case MIS:return Product.MIS;
		case NRML:return Product.NRML;
		default: throw new IllegalArgumentException(String.format("TradeProduct %s is not supported", tradeProduct.name()));
		}
	}
}
