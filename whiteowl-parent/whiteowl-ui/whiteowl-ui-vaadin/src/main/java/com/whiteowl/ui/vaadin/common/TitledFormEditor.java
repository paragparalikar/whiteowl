package com.whiteowl.ui.vaadin.common;

import java.util.function.Supplier;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.binder.Binder;

import lombok.Getter;

@Getter
public abstract class TitledFormEditor<T> extends TitledEditor {
	private static final long serialVersionUID = 5126259545788259305L;

	private T value;
	private final String entityName;
	private final Supplier<T> modelCreator;
	private final Binder<T> binder = new Binder<>();
	
	public TitledFormEditor(Icon icon, String entityName, Supplier<T> modelCreator) {
		setIcon(icon);
		this.entityName = entityName;
		this.modelCreator = modelCreator;
	}
	
	public void open(T value) {
		setError(null);
		binder.readBean(value);
		if(null == value) {
			this.value = modelCreator.get();
			setTitle(String.format("Create New %s", entityName));
		} else {
			this.value = value;
			setTitle(String.format("Edit %s", entityName));
		}
		open();
	}
	
	@Override
	protected void action() {
		try {
			binder.writeBean(value);
			edit(value);
			close();
		} catch (Exception e) {
			setError(e.getMessage());
		}
	}
	
	protected abstract void edit(T value);
}
