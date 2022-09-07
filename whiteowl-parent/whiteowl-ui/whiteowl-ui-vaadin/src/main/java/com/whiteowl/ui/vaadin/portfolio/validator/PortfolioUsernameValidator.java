package com.whiteowl.ui.vaadin.portfolio.validator;

import java.util.function.Supplier;

import org.springframework.util.StringUtils;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Credentials;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PortfolioUsernameValidator implements Validator<String> {
	private static final long serialVersionUID = 1L;
	
	private final PortfolioService portfolioService;
	private final Supplier<Portfolio> portfolioSupplier;
	private final Supplier<Broker> brokerSupplier;

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if(!StringUtils.hasText(value)) {
			return ValidationResult.error("Username can not be null/empty");
		}
		
		final Portfolio portfolio = portfolioSupplier.get();
		final Credentials credentials = null == portfolio ? null : portfolio.getCredentials();
		final String oldValue = null == credentials ? null : credentials.getUsername();
		final Long id = null == portfolio ? null : portfolio.getId();
		if(!value.equalsIgnoreCase(oldValue) && 
				portfolioService.existsByCredentialsUsernameIgnoreCaseAndBrokerAndIdNot(value, brokerSupplier.get(), id)) {
			return ValidationResult.error("Portfolio with this username already exits");
		}
		
		return ValidationResult.ok();
	}

}
