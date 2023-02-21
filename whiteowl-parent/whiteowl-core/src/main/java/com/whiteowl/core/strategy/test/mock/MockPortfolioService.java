package com.whiteowl.core.strategy.test.mock;

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
	public Portfolio save(@NonNull final Portfolio portfolio) {
		return this.portfolio = portfolio;
	}

	@Override
	public List<Portfolio> findAll() {
		return Collections.singletonList(portfolio);
	}

	@Override
	public Page<Portfolio> findAll(@NonNull final Pageable pageable) {
		return new PageImpl<>(findAll(), pageable, 1);
	}

	@Override
	public void delete(@NonNull final Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByNameIgnoreCaseAndIdNot(
			@NonNull final String name, @NonNull final Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(
			@NonNull final String username, 
			@NonNull final Broker broker, 
			@NonNull final Long id) {
		throw new UnsupportedOperationException();
	}

}
