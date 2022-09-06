package com.whiteowl.ui.vaadin.common;

import java.util.stream.Stream;

import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.ui.vaadin.util.VaadinUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScripDataProvider extends AbstractBackEndDataProvider<Scrip, String> {
	private static final long serialVersionUID = 1L;

	private final ScripService scripService;
	
	@Override
	protected Stream<Scrip> fetchFromBackEnd(Query<Scrip, String> query) {
		return scripService.findByIndices(Index.NIFTY50, VaadinUtils.toPageable(query)).get();
	}

	@Override
	protected int sizeInBackEnd(Query<Scrip, String> query) {
		return (int) scripService.countByIndices(Index.NIFTY50);
	}

}