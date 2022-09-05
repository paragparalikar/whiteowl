package com.whiteowl.data.chain;

import java.util.Set;

import com.whiteowl.core.scrip.Scrip;

public interface OptionChainPublisher {

	void subscribe(Set<Scrip> scrips, OptionChainSubscriber subscriber);
	
}
