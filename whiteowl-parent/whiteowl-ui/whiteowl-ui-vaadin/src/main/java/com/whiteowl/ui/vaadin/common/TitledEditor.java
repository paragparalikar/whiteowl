package com.whiteowl.ui.vaadin.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.whiteowl.ui.vaadin.util.VaadinUtils;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import lombok.Getter;

@Getter
public abstract class TitledEditor extends Dialog {
	private static final long serialVersionUID = -4106360760089133217L;
	
	private final H3 title = new H3();
	private final Span iconSpan = new Span();
	private final ErrorBar errorBar = new ErrorBar();
	private final HorizontalLayout header = new HorizontalLayout(iconSpan, title);
	private final FormLayout form = new FormLayout();
	private final VerticalLayout container = new VerticalLayout(header, errorBar, form, createButtonBar());

	public TitledEditor() {
		header.setWidthFull();
		title.getStyle()
			.set("font-size", "var(--lumo-font-size-xl)")
			.set("margin", "0");
		add(container);
	}
	
	public void setTitle(String value) {
		title.setText(value);
	}
	
	public void setIcon(Icon value) {
		iconSpan.add(value);
	}
	
	public void setError(String value) {
		errorBar.setError(value);
	}
	
	protected Component createButtonBar() {
		final Button cancelButton = new Button("Cancel", VaadinUtils.toIcon(VaadinIcon.CLOSE), event -> close());
		cancelButton.addClickShortcut(Key.ESCAPE);
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		final Button saveButton = new Button("Save", VaadinUtils.toIcon(VaadinIcon.DATABASE), event -> {
			try { action(); }
			catch(Exception e) { setError(e.getMessage()); }
		});
		saveButton.setAutofocus(true);
		saveButton.addClickShortcut(Key.ENTER);
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		final HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, saveButton);
		buttonsLayout.setWidthFull();
		buttonsLayout.setAlignItems(Alignment.END);
		buttonsLayout.setJustifyContentMode(JustifyContentMode.END);
		return buttonsLayout;
	}
	
	protected abstract void action();
}

