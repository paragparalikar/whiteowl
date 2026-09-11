package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
final class KiteScripMapper {

    private final KiteExchangeMapper exchangeMapper;
    private final Map<Integer, Scrip> instrumentTokenCache = new HashMap<>();
    private final Map<KiteExchange, Map<String, Scrip>> kiteKeyScripCache = new HashMap<>();
    private final Map<Exchange, Map<String, KiteSymbol>> exchangeKeySymbolCache = new HashMap<>();

    public Scrip toScrip(KiteSymbol kiteSymbol) {
        if (kiteSymbol == null) return null;
        Exchange exchange = exchangeMapper.toExchange(kiteSymbol.getExchange());
        String name = kiteSymbol.getName() == null ? kiteSymbol.getTradingsymbol() : kiteSymbol.getName();
        Scrip scrip = Scrip.builder()
                .id(exchange.name() + ":" + kiteSymbol.getTradingsymbol())
                .symbol(kiteSymbol.getTradingsymbol())
                .name(name)
                .exchange(exchange)
                .scripType(mapScripType(kiteSymbol))
                .tickSize(kiteSymbol.getTickSize())
                .lotSize(kiteSymbol.getLotSize())
                .build();
        cacheByToken(kiteSymbol.getInstrumentToken(), scrip);
        cacheByKiteKey(kiteSymbol.getExchange(), kiteSymbol.getTradingsymbol(), scrip);
        cacheByExchangeKey(exchange, scrip.getSymbol(), kiteSymbol);
        return scrip;
    }

    public Scrip getScrip(int instrumentToken) {
        return instrumentTokenCache.get(instrumentToken);
    }

    public Scrip getScrip(KiteExchange kiteExchange, String tradingsymbol) {
        return kiteKeyScripCache.getOrDefault(kiteExchange, Collections.emptyMap()).get(tradingsymbol);
    }

    public Scrip resolve(int instrumentToken, KiteExchange kiteExchange, String tradingsymbol) {
        Scrip scrip = getScrip(instrumentToken);
        if (scrip != null) return scrip;
        scrip = getScrip(kiteExchange, tradingsymbol);
        if (scrip != null) return scrip;
        return resolveByStrippingSuffix(kiteExchange, tradingsymbol);
    }

    private Scrip resolveByStrippingSuffix(KiteExchange kiteExchange, String tradingsymbol) {
        if (tradingsymbol == null || !tradingsymbol.matches(TRADING_SYMBOL_SUFFIX_PATTERN)) return null;
        String stripped = tradingsymbol.replaceAll(SUFFIX_PATTERN, "");
        return getScrip(kiteExchange, stripped);
    }

    public KiteSymbol getKiteSymbol(Exchange exchange, String symbol) {
        return exchangeKeySymbolCache.getOrDefault(exchange, Collections.emptyMap()).get(symbol);
    }

    public KiteSymbol getKiteSymbol(Scrip scrip) {
        return getKiteSymbol(scrip.getExchange(), scrip.getSymbol());
    }

    private void cacheByToken(int instrumentToken, Scrip scrip) {
        instrumentTokenCache.put(instrumentToken, scrip);
    }

    private void cacheByKiteKey(KiteExchange kiteExchange, String tradingsymbol, Scrip scrip) {
        kiteKeyScripCache.computeIfAbsent(kiteExchange, k -> new HashMap<>()).put(tradingsymbol, scrip);
    }

    private void cacheByExchangeKey(Exchange exchange, String symbol, KiteSymbol kiteSymbol) {
        exchangeKeySymbolCache.computeIfAbsent(exchange, k -> new HashMap<>()).put(symbol, kiteSymbol);
    }

    private static final String KITE_TYPE_EQ = "EQ";
    private static final String KITE_TYPE_FUT = "FUT";
    private static final String KITE_TYPE_CE = "CE";
    private static final String KITE_TYPE_PE = "PE";
    private static final String SEGMENT_INDICES = "INDICES";
    private static final String SEGMENT_CDS = "CDS";
    private static final String SEGMENT_BCD = "BCD";
    private static final String ETF_MARKER = "ETF";
    private static final String BEES_MARKER = "BEES";
    private static final String GOLD_MARKER = "GOLD";
    private static final String LIQUID_MARKER = "LIQUID";
    private static final String NIFTYBEES_MARKER = "NIFTYBEES";
    private static final String SUFFIX_PATTERN = "-[A-Z]{2}$";
    private static final String TRADING_SYMBOL_SUFFIX_PATTERN = ".*" + SUFFIX_PATTERN;
    private static final String SUFFIX_SG = "-SG";
    private static final String SUFFIX_GS = "-GS";
    private static final String SUFFIX_TB = "-TB";
    private static final String SUFFIX_NO = "-NO";

    private ScripType mapScripType(KiteSymbol kiteSymbol) {
        String kiteType = kiteSymbol.getType();
        String segment = kiteSymbol.getSegment();
        if (kiteType == null) return ScripType.OTHER;
        String upperType = kiteType.toUpperCase();
        if (SEGMENT_INDICES.equalsIgnoreCase(segment)) return ScripType.INDEX;
        if (SEGMENT_CDS.equalsIgnoreCase(segment) || SEGMENT_BCD.equalsIgnoreCase(segment)) return ScripType.CURRENCY;
        return switch (upperType) {
            case KITE_TYPE_FUT -> ScripType.FUTURES;
            case KITE_TYPE_CE, KITE_TYPE_PE -> ScripType.OPTIONS;
            case KITE_TYPE_EQ -> classifyEquity(kiteSymbol);
            default -> ScripType.OTHER;
        };
    }

    private ScripType classifyEquity(KiteSymbol kiteSymbol) {
        String symbol = kiteSymbol.getTradingsymbol().toUpperCase();
        String name = kiteSymbol.getName() != null ? kiteSymbol.getName().toUpperCase() : "";
        if (isDebtInstrument(symbol)) return ScripType.DEBT;
        if (isEtfLike(symbol) || isEtfLike(name)) return ScripType.ETF;
        return ScripType.EQUITY;
    }

    private boolean isDebtInstrument(String symbol) {
        return symbol.endsWith(SUFFIX_SG) || symbol.endsWith(SUFFIX_GS)
                || symbol.endsWith(SUFFIX_TB) || symbol.endsWith(SUFFIX_NO);
    }

    private boolean isEtfLike(String text) {
        return text.contains(ETF_MARKER) || text.contains(BEES_MARKER)
                || text.contains(GOLD_MARKER) || text.contains(LIQUID_MARKER)
                || text.contains(NIFTYBEES_MARKER);
    }

}
