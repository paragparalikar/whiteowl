package com.whiteowl.ui.vaadin.common;

import org.springframework.security.core.userdetails.UserDetails;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLink;
import com.whiteowl.core.user.User;
import com.whiteowl.ui.vaadin.DashboardView;
import com.whiteowl.ui.vaadin.UiConstants;
import com.whiteowl.ui.vaadin.portfolio.PortfolioGridView;
import com.whiteowl.ui.vaadin.security.SecurityService;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

public class Navigation extends VerticalLayout implements AfterNavigationObserver {
	private static final long serialVersionUID = 8891049443289300801L;

	private final Label usernameLabel;
	private final SecurityService securityService;
	private final Tab dashboardTab = new Tab(toIcon(VaadinIcon.DASHBOARD), new RouterLink("Dashboard", DashboardView.class));
	private final Tab portfolioTab = new Tab(toIcon(VaadinIcon.SUITCASE), new RouterLink("Portfolios", PortfolioGridView.class));
	private final Tabs tabs = new Tabs();
	
	public Navigation(SecurityService securityService, final Label usernameLabel) {
		this.usernameLabel = usernameLabel;
		this.securityService = securityService;
		setHeightFull();
		add(tabs);
		expand(tabs);
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
	}
	
	private Icon toIcon(VaadinIcon vaadinIcon) {
		final Icon icon = VaadinUtils.toIcon(vaadinIcon);
		icon.setSize("2em");
		return icon;
	}
	
	private void init() {
		final UserDetails userDetails = securityService.getAuthenticatedUser();
		if(0 == tabs.getComponentCount() && null != userDetails) {
			if(userDetails instanceof User) {
				usernameLabel.setText("Welcome " + User.class.cast(userDetails).getDisplayName());
			} else {
				usernameLabel.setText("Welcome " + userDetails.getUsername());
			}
			tabs.add(dashboardTab);
			tabs.add(portfolioTab);
		}
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		init();
		final String segment = event.getLocation().getFirstSegment();
		switch(segment) {
		case UiConstants.ROUTE_DASHBOARD : tabs.setSelectedTab(dashboardTab); break;
		case UiConstants.ROUTE_PORTFOLIO : tabs.setSelectedTab(portfolioTab); break;
		}
	}

}
