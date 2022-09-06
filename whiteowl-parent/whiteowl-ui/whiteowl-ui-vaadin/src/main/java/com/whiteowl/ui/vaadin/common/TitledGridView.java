package com.whiteowl.ui.vaadin.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

import lombok.Getter;

@Getter
public abstract class TitledGridView<T> extends TitledView {
	private static final long serialVersionUID = 7661138175543626984L;

	public TitledGridView(Icon icon, String title) {
		super(icon, title);
	}
	
	protected void createColumns(Grid<T> grid) {
		grid.addComponentColumn(this::createActionCell)
			.setSortable(false)
			.setHeader(createActionColumnHeader())
			.setTextAlign(ColumnTextAlign.END)
			.setAutoWidth(true);
	}
	
	protected Component createActionColumnHeader() {
		final Button button = new Button("New", VaadinUtils.toIcon(VaadinIcon.PLUS));
		button.addClickListener(event -> create());
		return button;
	}
	
	protected HorizontalLayout createActionCell(T value) {
		final Button editButton = new Button("Edit", VaadinUtils.toIcon(VaadinIcon.EDIT), event -> edit(value));
		final Button deleteButton = new Button("Delete", VaadinUtils.toIcon(VaadinIcon.TRASH), event -> delete(value));
		final HorizontalLayout container = new HorizontalLayout(editButton, deleteButton);
		container.setWidthFull();
		container.setJustifyContentMode(JustifyContentMode.END);
		return container;
	}
	
	protected abstract void create();
	protected abstract void edit(T value);
	protected abstract void delete(T value);
	
}
