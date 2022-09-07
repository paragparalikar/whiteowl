package com.whiteowl.core.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.whiteowl.core.broker.Broker;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
	
	boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(String username, Broker broker, Long id);
	
}
