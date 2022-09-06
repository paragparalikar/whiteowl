package com.whiteowl.ui.vaadin.common;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class ErrorBar extends HorizontalLayout {
	private static final long serialVersionUID = -1468577223141360166L;

	private final Label textLabel = new Label();
	private final Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
	
	public ErrorBar() {
		setWidthFull();
		setVisible(false);
		add(textLabel, closeButton);
		textLabel.setHeightFull();
		textLabel.getStyle().set("padding-left", "1em");
		getStyle().set("background-color", "#E8563F");
		getStyle().set("color", "#ffffff");
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.BETWEEN);
		closeButton.addClickListener(event -> ErrorBar.this.setVisible(false));
	}
	
	public void setError(String value) {
		textLabel.setText(value);
		setVisible(StringUtils.hasText(value));
	}
	
}
