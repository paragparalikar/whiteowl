package com.whiteowl.ui.vaadin.portfolio;

import java.util.stream.Stream;

import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PortfolioDataProvider extends AbstractBackEndDataProvider<Portfolio, Void> {
	private static final long serialVersionUID = 1L;

	@NonNull private final PortfolioService portfolioService;

	@Override
	protected Stream<Portfolio> fetchFromBackEnd(Query<Portfolio, Void> query) {
		return portfolioService.findAll(VaadinUtils.toPageable(query)).get();
	}

	@Override
	protected int sizeInBackEnd(Query<Portfolio, Void> query) {
		return portfolioService.count();
	}

}
