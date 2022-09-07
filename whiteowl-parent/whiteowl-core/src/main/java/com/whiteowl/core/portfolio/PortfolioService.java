package com.whiteowl.core.portfolio;

import java.util.List;

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
		return portfolioRepository.findAll();
	}
	
	public Portfolio save(Portfolio portfolio) {
		return  portfolioRepository.saveAndFlush(portfolio);
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
	
	public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
		id = null == id ? -1l : id;
		return portfolioRepository.existsByNameIgnoreCaseAndIdNot(name, id);
	}
	
	public boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, Broker broker, Long id) {
		id = null == id ? -1l : id;
		return portfolioRepository.existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(username, broker, id);
	}
	
}
