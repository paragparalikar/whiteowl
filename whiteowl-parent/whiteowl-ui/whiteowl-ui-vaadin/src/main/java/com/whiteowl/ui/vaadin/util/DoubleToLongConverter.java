package com.whiteowl.ui.vaadin.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class DoubleToLongConverter implements Converter<Double, Long> {
	private static final long serialVersionUID = -2370152333406590161L;

	@Override
	public Result<Long> convertToModel(Double value, ValueContext context) {
		if(null == value) return Result.ok(null);
		if(((double)value.longValue()) < value) {
			return Result.error("Value must be a whole number");
		}
		return Result.ok(value.longValue());
	}

	@Override
	public Double convertToPresentation(Long value, ValueContext context) {
		return null == value ? null : value.doubleValue();
	}

}
