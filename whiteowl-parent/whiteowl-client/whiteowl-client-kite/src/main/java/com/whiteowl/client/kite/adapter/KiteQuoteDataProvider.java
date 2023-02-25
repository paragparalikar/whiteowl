package com.whiteowl.client.kite.adapter;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteDataProvider;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.scrip.Scrip;

@Component
public class KiteQuoteDataProvider implements QuoteDataProvider {

	private final KiteMapper kiteMapper;
	private final KiteConnectApi kiteClient;
	private final KiteInstrumentService kiteInstrumentService;
	private final Map<Consumer<Quote>, Consumer<KiteTick>> quoteListeners = new IdentityHashMap<>();
	
	public KiteQuoteDataProvider(KiteMapper kiteMapper, KiteClientProvider kiteClientProvider,
			KiteInstrumentService kiteInstrumentService, KiteDataProviderCredentials kiteDataProviderCredentials) {
		super();
		this.kiteMapper = kiteMapper;
		this.kiteInstrumentService = kiteInstrumentService;
		this.kiteClient = kiteClientProvider.getClient(kiteDataProviderCredentials.toKiteCredentials());
	}
	
	@Override
	public Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode) {
		final KiteQuoteMode kiteQuoteMode = kiteMapper.toKiteQuoteMode(mode);
		final Set<Instrument> instruments = scrips.stream()
			.map(Scrip::getCode)
			.map(kiteInstrumentService::findByTradingSymbol)
			.collect(Collectors.toSet());
		final Collection<KiteQuote> kiteQuotes = kiteClient.getQuotes(instruments, kiteQuoteMode);
		return kiteQuotes.stream()
				.map(kiteMapper::toQuote)
				.collect(Collectors.toSet());
	}

	@Override
	public void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		final KiteQuoteMode kiteQuoteMode = kiteMapper.toKiteQuoteMode(mode);
		final Set<Instrument> instruments = scrips.stream()
				.map(Scrip::getCode)
				.map(kiteInstrumentService::findByTradingSymbol)
				.collect(Collectors.toSet());
		kiteClient.subscribeTickListener(instruments, kiteQuoteMode, 
				quoteListeners.computeIfAbsent(quoteListener, this::toTickListener));
	}
	
	private Consumer<KiteTick> toTickListener(Consumer<Quote> quoteListener){
		return tick -> quoteListener.accept(kiteMapper.toQuote(tick));
	}

	@Override
	public void unsubscribe(Consumer<Quote> quoteListener) {
		kiteClient.unsubscribeTickListener(quoteListeners.get(quoteListener));
	}

}
