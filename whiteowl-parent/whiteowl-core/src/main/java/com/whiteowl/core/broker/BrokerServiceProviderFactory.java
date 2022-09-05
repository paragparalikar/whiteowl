package com.whiteowl.core.broker;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BrokerServiceProviderFactory {

	private final Map<Broker, BrokerServiceProvider> cache = new EnumMap<>(Broker.class);
	
	public BrokerServiceProviderFactory(List<BrokerServiceProvider> delegates) {
		delegates.forEach(delegate -> cache.put(delegate.getBrokerType(), delegate));
	}
	
	public BrokerServiceProvider getBrokerServiceProvider(Broker broker) {
		return cache.get(broker);
	}
}
