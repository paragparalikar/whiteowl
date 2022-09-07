package com.whiteowl.core.portfolio;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
@EnableScanCount
public interface PortfolioRepository extends PagingAndSortingRepository<Portfolio, String> {

	boolean existsByNameLowerCaseAndIdNot(String name, String id);
	
}
