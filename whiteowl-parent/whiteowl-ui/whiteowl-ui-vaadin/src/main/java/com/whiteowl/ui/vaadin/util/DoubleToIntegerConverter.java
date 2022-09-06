package com.whiteowl.ui.vaadin.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class DoubleToIntegerConverter implements Converter<Double, Integer> {
	private static final long serialVersionUID = -6805590239449691183L;

	@Override
	public Result<Integer> convertToModel(Double value, ValueContext context) {
		if(null == value) return Result.ok(null);
		if(((double)value.intValue()) < value) {
			return Result.error("Value must be a whole number");
		}
		return Result.ok(value.intValue());
	}

	@Override
	public Double convertToPresentation(Integer value, ValueContext context) {
		return null == value ? null : value.doubleValue();
	}

}
