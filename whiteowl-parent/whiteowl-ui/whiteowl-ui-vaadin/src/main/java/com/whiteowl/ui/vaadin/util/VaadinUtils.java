package com.whiteowl.ui.vaadin.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VaadinUtils {
	
	public final String ICON_COLOR = SolidColor.GOLD.toString();

	public Pageable toPageable(Query<?, ?> query) {
		final List<Order> orders = Optional.ofNullable(query.getSortOrders()).orElse(Collections.emptyList()).stream()
			.map(order -> new Order(toDirection(order.getDirection()), order.getSorted()))
			.collect(Collectors.toList());
		return PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(orders));
	}
	
	public Direction toDirection(SortDirection direction) {
		if(null == direction) return Direction.ASC;
		switch (direction) {
		case DESCENDING: return Direction.DESC;
		case ASCENDING: return Direction.ASC;
		default: return Direction.ASC;
		}
	}
	
	public Icon toIcon(VaadinIcon vaadinIcon) {
		final Icon icon = new Icon(vaadinIcon);
		icon.setColor(ICON_COLOR);
		return icon;
	}
	
}
