package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.client.kite.symbol.KiteFileSystemScripLoader;
import com.whiteowl.client.kite.symbol.KiteScripLoader;
import com.whiteowl.core.broker.ScripLoader;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.List;

public final class KiteScripLoaderAdapter implements ScripLoader {

    private final KiteFileSystemScripLoader fsLoader;
    private final KiteMapper mapper;

    public KiteScripLoaderAdapter() {
        this(new KiteFileSystemScripLoader(), KiteMapper.INSTANCE);
    }

    public KiteScripLoaderAdapter(KiteFileSystemScripLoader fsLoader, KiteMapper mapper) {
        this.fsLoader = fsLoader;
        this.mapper = mapper;
    }

    @Override
    public List<Scrip> loadByExchange(Exchange exchange) {
        return loadByExchange(exchange, false);
    }

    @Override
    public List<Scrip> loadByExchange(Exchange exchange, boolean forceDownload) {
        KiteExchange kiteExchange = mapper.toKiteExchange(exchange);
        List<KiteSymbol> kiteSymbols = fsLoader.loadByExchange(kiteExchange, forceDownload);
        return kiteSymbols.stream()
                .map(mapper::toScrip)
                .toList();
    }

}
