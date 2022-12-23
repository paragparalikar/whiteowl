package com.whiteowl.ui.vaadin.portfolio;

import javax.annotation.security.PermitAll;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.ui.vaadin.UiConstants;
import com.whiteowl.ui.vaadin.common.MainLayout;
import com.whiteowl.ui.vaadin.common.TitledGridView;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

@PermitAll
@PageTitle("Portfolios")
@Route(value = UiConstants.ROUTE_PORTFOLIO, layout = MainLayout.class)
public class PortfolioGridView extends TitledGridView<Portfolio> {
	private static final long serialVersionUID = 1L;

	private final PortfolioEditor portfolioEditor;
	private final PortfolioService portfolioService;
	private final DataProvider<Portfolio, Void> portfolioDataProvider;
	
	public PortfolioGridView(PortfolioService portfolioService) {
		super(VaadinUtils.toIcon(VaadinIcon.SUITCASE), "Portfolio");
		this.portfolioService = portfolioService;
		this.portfolioDataProvider = new PortfolioDataProvider(portfolioService);
		this.portfolioEditor = new PortfolioEditor(portfolioService, portfolioDataProvider);
		final Grid<Portfolio> grid = new Grid<>();
		grid.setItems(portfolioDataProvider);
		createColumns(grid);
		add(grid, portfolioEditor);
	}
	
	@Override
	protected void createColumns(Grid<Portfolio> grid) {
		grid.addColumn(Portfolio::getId, "id").setHeader("Id");
		grid.addColumn(Portfolio::getName, "name").setHeader("Name");
		grid.addColumn(Portfolio::getBroker, "broker").setHeader("Broker");
		grid.addColumn(Portfolio::getMaxTradableAmount, "maxTradableAmount").setHeader("Amount");
		super.createColumns(grid);
	}
	
	@Override
	protected void create() {
		this.portfolioEditor.open(null);
	}

	@Override
	protected void edit(Portfolio portfolio) {
		this.portfolioEditor.open(portfolio);
	}

	@Override
	protected void delete(Portfolio portfolio) {
		try {
			portfolioService.delete(portfolio);
			portfolioDataProvider.refreshAll();
		} catch(Exception e) {
			setError(e.getMessage());
		}
	}

}
