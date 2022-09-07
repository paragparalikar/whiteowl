package com.whiteowl.core.portfolio;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.whiteowl.core.broker.Broker;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;
	
	public List<Portfolio> findAll(){
		return StreamSupport.stream(portfolioRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
	}
	
	public Portfolio save(Portfolio portfolio) {
		return  portfolioRepository.save(portfolio);
	}

	public Page<Portfolio> findAll(Pageable pageable) {
		return portfolioRepository.findAll(pageable);
	}

	public int count() {
		return (int) portfolioRepository.count();
	}
	
	public void delete(Portfolio portfolio) {
		portfolioRepository.delete(portfolio);
	}
	
	public boolean existsByName(String name) {
		return portfolioRepository.existsByName(name);
	}
	
	public boolean existsByCredentialsUsernameAndBroker(String username, Broker broker) {
		return portfolioRepository.existsByCredentialsUsernameAndBroker(username, broker);
	}
	
}
