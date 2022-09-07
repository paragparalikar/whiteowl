package com.whiteowl.core.bar;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface BarRepository extends CrudRepository<PersistentBar, String> {
	
	
}

