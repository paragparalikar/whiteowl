package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.core.scrip.Exchange;

public class ExchangeMapper {

	public Exchange toExchange(KiteExchange kiteExchange) {
		if(null == kiteExchange) return null;
		switch(kiteExchange) {
		case BCD: return Exchange.BCD;
		case MCX: return Exchange.MCX;
		case BFO:return Exchange.BFO;
		case BSE:return Exchange.BSE;
		case CDS:return Exchange.CDS;
		case MF:return Exchange.MF;
		case NFO:return Exchange.NFO;
		case NSE:return Exchange.NSE;
		default:return null;
		}
	}
	
	public KiteExchange toKiteExchange(Exchange exchange) {
		if(null == exchange) return null;
		switch(exchange) {
		case BCD: return KiteExchange.BCD;
		case MCX: return KiteExchange.MCX;
		case BFO:return KiteExchange.BFO;
		case BSE:return KiteExchange.BSE;
		case CDS:return KiteExchange.CDS;
		case MF:return KiteExchange.NFO;
		case NFO:return KiteExchange.NFO;
		case NSE:return KiteExchange.NSE;
		default: return null;
		}
	}
	
}
