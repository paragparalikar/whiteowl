package com.whiteowl.core.portfolio;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.whiteowl.core.broker.Broker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class DefaultPortfolioService implements PortfolioService {

	private final PortfolioRepository portfolioRepository;
	
	@Override
	public List<Portfolio> findAll(){
		return portfolioRepository.findAll();
	}
	
	@Override
	public Portfolio save(@Valid @NonNull final Portfolio portfolio) {
		return  portfolioRepository.saveAndFlush(portfolio);
	}

	@Override
	public Page<Portfolio> findAll(@NonNull final Pageable pageable) {
		return portfolioRepository.findAll(pageable);
	}

	@Override
	public int count() {
		return (int) portfolioRepository.count();
	}
	
	@Override
	public void delete(@NonNull final Portfolio portfolio) {
		portfolioRepository.delete(portfolio);
	}
	
	@Override
	public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
		id = null == id ? -1l : id;
		return portfolioRepository.existsByNameIgnoreCaseAndIdNot(name, id);
	}
	
	@Override
	public boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, Broker broker, Long id) {
		id = null == id ? -1l : id;
		return portfolioRepository.existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(username, broker, id);
	}
	
}
