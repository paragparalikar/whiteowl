package com.whiteowl.core.portfolio;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.whiteowl.core.broker.Broker;

@Repository
@EnableScan
@EnableScanCount
public interface PortfolioRepository extends PagingAndSortingRepository<Portfolio, String> {

	boolean existsByName(String name);
	
	boolean existsByCredentialsUsernameAndBroker(String username, Broker broker);
	
}
