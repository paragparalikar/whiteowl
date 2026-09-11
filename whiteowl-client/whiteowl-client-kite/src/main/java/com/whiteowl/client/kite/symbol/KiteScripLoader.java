package com.whiteowl.client.kite.symbol;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;

import java.util.List;

public interface KiteScripLoader {

    List<KiteSymbol> loadByExchange(KiteExchange exchange);

}
