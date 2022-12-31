package com.whiteowl.strategy.test.mock;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class MockPortfolioService implements PortfolioService {
	
	@NonNull private Portfolio portfolio;
	
	@Override
	public int count() {
		return 1;
	}

	@Override
	public Portfolio save(Portfolio portfolio) {
		return this.portfolio = portfolio;
	}

	@Override
	public List<Portfolio> findAll() {
		return Collections.singletonList(portfolio);
	}

	@Override
	public Page<Portfolio> findAll(Pageable pageable) {
		return new PageImpl<>(findAll(), pageable, 1);
	}

	@Override
	public void delete(Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, Broker broker, Long id) {
		throw new UnsupportedOperationException();
	}

}
