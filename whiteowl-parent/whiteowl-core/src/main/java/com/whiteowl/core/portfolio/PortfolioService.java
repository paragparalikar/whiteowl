package com.whiteowl.core.portfolio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.broker.Broker;

public interface PortfolioService {
	
	int count();
	
	Portfolio save(Portfolio portfolio);
	
	List<Portfolio> findAll();
	
	Page<Portfolio> findAll(Pageable pageable);
	
	void delete(Portfolio portfolio);
	
	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
	
	boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, Broker broker, Long id);

}
