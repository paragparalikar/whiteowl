package com.whiteowl.ui.vaadin.portfolio.validator;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

public class PortfolioAmountValidator implements Validator<Double> {
	private static final long serialVersionUID = 1L;

	@Override
	public ValidationResult apply(Double value, ValueContext context) {
		if(0 >= value) {
			return ValidationResult.error("Amount must be positive");
		}
		return ValidationResult.ok();
	}

}
