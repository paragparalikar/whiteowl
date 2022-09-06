package com.whiteowl.ui.vaadin;

import javax.annotation.security.PermitAll;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.whiteowl.ui.vaadin.common.MainLayout;

@PermitAll
@PageTitle("Dashboard")
@Route(value = UiConstants.ROUTE_DASHBOARD, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {
	private static final long serialVersionUID = 1L;


}
