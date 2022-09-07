package com.whiteowl.core.portfolio;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;
	
	public List<Portfolio> findAll(){
		return StreamSupport.stream(portfolioRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
	}
	
	public Portfolio save(@Valid Portfolio portfolio) {
		final Credentials credentials = portfolio.getCredentials();
		credentials.setUsername(credentials.getUsername().toUpperCase());
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
	
	public boolean existsByNameAndIdNot(String name, String id) {
		id = null == id ? "null" : id;
		name = null == name ? null : name.toLowerCase();
		return portfolioRepository.existsByNameLowerCaseAndIdNot(name, id);
	}
	
}
