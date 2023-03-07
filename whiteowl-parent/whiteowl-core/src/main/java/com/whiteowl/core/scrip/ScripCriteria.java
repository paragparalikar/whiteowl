package com.whiteowl.core.scrip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ScripCriteria implements Predicate<Scrip> {
	
	public static final Predicate<Scrip> INSTANCE = new ScripCriteria()
			.withIndex(Index.NIFTY50)
			.or(new ScripCriteria()
					.withCode(Index.NIFTY50.getCode())
					.withCode(Index.NIFTYBANK.getCode())
					.withCode(Index.VIX.getCode()));
	
	private final Set<String> codes = new HashSet<>();
	private final Set<Index> indices = new HashSet<>();
	private final Set<Segment> segments = new HashSet<>();
	private final Set<Expiry> expiries = new HashSet<>();
	private final Set<Exchange> exchanges = new HashSet<>();
	private final Set<ScripType> scripTypes = new HashSet<>();
	private final Set<StrikeType> strikeTypes = new HashSet<>();
	private final Set<ExpiryType> expiryTypes = new HashSet<>();
	
	@Override
	public boolean test(Scrip scrip) {
		return  (codes.isEmpty() || codes.contains(scrip.getCode())) &&
				(indices.isEmpty() || !Collections.disjoint(indices, scrip.getIndices())) &&
				(segments.isEmpty() || segments.contains(scrip.getSegment())) &&
				(exchanges.isEmpty() || exchanges.contains(scrip.getExchange())) &&
				(scripTypes.isEmpty() || scripTypes.contains(scrip.getType()));
	}
	
	public ScripCriteria withCode(String code) {
		codes.add(code);
		return this;
	}
	
	public ScripCriteria withIndex(Index index) {
		indices.add(index);
		return this;
	}
	
	public ScripCriteria withSegment(Segment segment) {
		segments.add(segment);
		return this;
	}
	
	public ScripCriteria withExpiry(Expiry expiry) {
		expiries.add(expiry);
		return this;
	}
	
	public ScripCriteria withExchange(Exchange exchange) {
		exchanges.add(exchange);
		return this;
	}
	
	public ScripCriteria withScripType(ScripType scripType) {
		scripTypes.add(scripType);
		return this;
	}
	
	public ScripCriteria withStrikeType(StrikeType strikeType) {
		strikeTypes.add(strikeType);
		return this;
	}
	
	public ScripCriteria withExpiryType(ExpiryType expiryType) {
		expiryTypes.add(expiryType);
		return this;
	}
}
