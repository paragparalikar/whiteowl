package com.whiteowl.ui.vaadin.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TitledView extends VerticalLayout {
	private static final long serialVersionUID = 8809968448449554091L;

	private final H2 title = new H2();
	private final Span iconSpan = new Span();
	private final ErrorBar errorBar = new ErrorBar();
	private final HorizontalLayout left = new HorizontalLayout(iconSpan, title);
	private final HorizontalLayout right = new HorizontalLayout();
	private final HorizontalLayout header = new HorizontalLayout(left, right);
	
	public TitledView() {
		setHeightFull();
		header.setWidthFull();
		title.getStyle()
			.set("font-size", "var(--lumo-font-size-xl)")
			.set("margin", "0");
		errorBar.setWidthFull();
		left.setJustifyContentMode(JustifyContentMode.START);
		left.setWidth(50, Unit.PERCENTAGE);
		right.setJustifyContentMode(JustifyContentMode.END);
		right.setWidth(50, Unit.PERCENTAGE);
		add(errorBar, header);
	}
	
	public TitledView(Icon icon, String title) {
		this();
		setIcon(icon);
		setTitle(title);
	}
	
	public void setTitle(String value) {
		title.setText(value);
	}
	
	public void setIcon(Icon value) {
		iconSpan.add(value);
	}
	
	public void addRight(Component component) {
		right.add(component);
	}
	
	public void setError(String value) {
		errorBar.setError(value);
	}
}
