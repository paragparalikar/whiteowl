package com.whiteowl.core.user;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
@EnableScanCount
public interface UserRepository extends PagingAndSortingRepository<User, String> {
	
}
