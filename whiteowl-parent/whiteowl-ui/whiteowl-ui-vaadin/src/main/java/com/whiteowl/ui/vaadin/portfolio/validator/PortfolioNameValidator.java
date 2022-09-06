package com.whiteowl.ui.vaadin.portfolio.validator;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.util.StringUtils;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PortfolioNameValidator implements Validator<String> {
	private static final long serialVersionUID = 1L;

	private final PortfolioService portfolioService;
	private final Supplier<Portfolio> portfolioSupplier;
	
	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if(!StringUtils.hasText(value)) {
			return ValidationResult.error("Name can not be null/empty");
		}
		
		final String oldValue = Optional.ofNullable(portfolioSupplier.get())
				.map(Portfolio::getName)
				.orElse(null);
		if(!value.equalsIgnoreCase(oldValue) && portfolioService.existsByNameIgnoreCase(value)) {
			return ValidationResult.error("Portfolio with this name already exists");
		}
		
		return ValidationResult.ok();
	}

}
