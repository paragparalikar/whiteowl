package com.whiteowl.core.strategy.config.backtester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;

import lombok.NonNull;

public class MockPortfolioService implements PortfolioService {

	private final AtomicLong idGenerator = new AtomicLong(1l);
	private final Set<Portfolio> cache = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	@Override
	public int count() {
		return cache.size();
	}

	@Override
	public List<Portfolio> findAll() {
		return new ArrayList<>(cache);
	}

	@Override
	public void delete(@NonNull Portfolio portfolio) {
		cache.remove(portfolio);
	}

	@Override
	public Portfolio save(@Valid @NonNull Portfolio portfolio) {
		if(null == portfolio.getId()) portfolio.setId(idGenerator.getAndIncrement());
		cache.add(portfolio);
		return portfolio;
	}

	@Override
	public Page<Portfolio> findAll(@NonNull Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
		return cache.stream().anyMatch(portfolio -> 
					name.trim().equalsIgnoreCase(portfolio.getName().trim())
					&& !Objects.equals(id, portfolio.getId()));
	}

	@Override
	public boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, @NonNull Broker broker,
			Long id) {
		return cache.stream().anyMatch(portfolio -> 
				username.trim().equalsIgnoreCase(portfolio.getCredentials().getUsername().trim())
				&& Objects.equals(broker, portfolio.getBroker())
				&& !Objects.equals(id, portfolio.getId()));
	}

}
