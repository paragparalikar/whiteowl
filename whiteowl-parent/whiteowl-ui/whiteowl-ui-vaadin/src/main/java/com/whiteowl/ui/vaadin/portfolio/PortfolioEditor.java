package com.whiteowl.ui.vaadin.portfolio;

import java.util.Arrays;
import java.util.Optional;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Credentials;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.ui.vaadin.common.TitledFormEditor;
import com.whiteowl.ui.vaadin.portfolio.validator.PortfolioAmountValidator;
import com.whiteowl.ui.vaadin.portfolio.validator.PortfolioNameValidator;
import com.whiteowl.ui.vaadin.portfolio.validator.PortfolioUsernameValidator;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

public class PortfolioEditor extends TitledFormEditor<Portfolio> {
	private static final long serialVersionUID = 1L;

	private final PortfolioService portfolioService;
	private final DataProvider<Portfolio, Void> dataProvider;
	
	public PortfolioEditor(PortfolioService portfolioService, DataProvider<Portfolio, Void> dataProvider) {
		super(VaadinUtils.toIcon(VaadinIcon.SUITCASE), "Portfolio", Portfolio::new);
		this.portfolioService = portfolioService;
		this.dataProvider = dataProvider;
		createForm(getBinder(), getForm());
	}
	
	private void createForm(Binder<Portfolio> binder, FormLayout layout) {
		final TextField nameField = new TextField("Name");
		nameField.setWidthFull();
		binder.forField(nameField)
			.asRequired("Portfolio name is required")
			.withValidator(new PortfolioNameValidator(portfolioService, this::getValue))
			.bind(Portfolio::getName, Portfolio::setName);
		nameField.setPrefixComponent(VaadinUtils.toIcon(VaadinIcon.USER));
		
		final ComboBox<Broker> brokerComboBox = new ComboBox<>("Broker", Arrays.asList(Broker.values()));
		brokerComboBox.setWidthFull();
		binder.forField(brokerComboBox)
			.asRequired("Broker is required")
			.bind(Portfolio::getBroker, Portfolio::setBroker);
		
		final NumberField amountField = new NumberField("Max Usable Amount");
		amountField.setWidthFull();
		binder.forField(amountField)
			.asRequired("Max Usable Amount is required")
			.withValidator(new PortfolioAmountValidator())
			.bind(Portfolio::getMaxTradableAmount, Portfolio::setMaxTradableAmount);
		amountField.setPrefixComponent(VaadinUtils.toIcon(VaadinIcon.MONEY));
		
		final TextField usernameField = new TextField("Username");
		usernameField.setWidthFull();
		binder.forField(usernameField)
			.asRequired("Username is required")
			.withValidator(new PortfolioUsernameValidator(portfolioService, this::getValue, brokerComboBox::getValue))
			.bind(portfolio -> Optional.ofNullable(portfolio).map(Portfolio::getCredentials).map(Credentials::getUsername).orElse(null), 
					(portfolio, usernamme) -> portfolio.getCredentials().setUsername(usernamme));
		usernameField.setPrefixComponent(VaadinUtils.toIcon(VaadinIcon.USER));
		
		final PasswordField passwordField = new PasswordField("Password");
		passwordField.setWidthFull();
		binder.forField(passwordField)
			.asRequired("Password is required")
			.bind(portfolio -> Optional.ofNullable(portfolio).map(Portfolio::getCredentials).map(Credentials::getPassword).orElse(null), 
					(portfolio, password) -> portfolio.getCredentials().setPassword(password));
		passwordField.setPrefixComponent(VaadinUtils.toIcon(VaadinIcon.KEY));
		
		final PasswordField pinField = new PasswordField("Pin");
		pinField.setWidthFull();
		binder.forField(pinField)
		.asRequired("Pin is required")
		.bind(portfolio -> Optional.ofNullable(portfolio).map(Portfolio::getCredentials).map(Credentials::getPin).orElse(null), 
				(portfolio, pin) -> portfolio.getCredentials().setPin(pin));
		pinField.setPrefixComponent(VaadinUtils.toIcon(VaadinIcon.KEY));

		
		final VerticalLayout detailsLayout = new VerticalLayout(nameField, brokerComboBox, amountField);
		final VerticalLayout credentialsLayout = new VerticalLayout(usernameField, passwordField, pinField);
		final HorizontalLayout container = new HorizontalLayout(detailsLayout, credentialsLayout);
		layout.setWidth("60em");
		layout.add(container);
		container.setMinWidth("100%");
		detailsLayout.setWidth("50%");
		credentialsLayout.setWidth("50%");
	}
	
	
	@Override
	protected void edit(Portfolio portfolio) {
		portfolioService.save(portfolio);
		dataProvider.refreshAll();
	}

}
