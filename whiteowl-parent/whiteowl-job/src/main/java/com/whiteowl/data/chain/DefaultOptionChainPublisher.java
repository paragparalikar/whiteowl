package com.whiteowl.data.chain;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.whiteowl.core.scrip.Scrip;

public class DefaultOptionChainPublisher implements OptionChainPublisher {
	
	private final Map<OptionChainSubscriber, Set<Scrip>> subscribers = new IdentityHashMap<>();

	@Override
	public void subscribe(Set<Scrip> scrips, OptionChainSubscriber subscriber) {
		// TODO Auto-generated method stub

	}

}
