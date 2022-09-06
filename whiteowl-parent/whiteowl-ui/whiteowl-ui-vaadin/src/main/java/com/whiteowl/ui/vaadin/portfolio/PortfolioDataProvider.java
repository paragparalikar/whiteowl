package com.whiteowl.ui.vaadin.portfolio;

import java.util.stream.Stream;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class PortfolioDataProvider implements DataProvider<Portfolio, Void> {
	private static final long serialVersionUID = 1L;

	@NonNull private final PortfolioService portfolioService;
	@Delegate private final DataProvider<Portfolio, Void> delegate = 
			DataProvider.fromCallbacks(this::findByQuery, this::countByQuery);
	
	private int countByQuery(Query<Portfolio, Void> query) {
		return (int) portfolioService.count();
	}

	private Stream<Portfolio> findByQuery(Query<Portfolio, Void> query) {
		return portfolioService.findAll(VaadinUtils.toPageable(query)).stream();
	}

}
