package com.whiteowl.core.portfolio;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.broker.Broker;

import lombok.NonNull;

public interface PortfolioService {
	
	int count();
	
	List<Portfolio> findAll();
	
	void delete(@NonNull final Portfolio portfolio);
	
	Portfolio save(@Valid @NonNull final Portfolio portfolio);
	
	Page<Portfolio> findAll(@NonNull final Pageable pageable);
	
	boolean existsByNameIgnoreCaseAndIdNot(final String name, final Long id);
	
	boolean existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(final String username, @NonNull final Broker broker, final Long id);

}
