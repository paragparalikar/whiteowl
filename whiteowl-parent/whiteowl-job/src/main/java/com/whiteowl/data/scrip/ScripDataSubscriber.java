package com.whiteowl.data.scrip;

import java.util.List;

import com.whiteowl.core.scrip.Scrip;

public interface ScripDataSubscriber {

	void onScrips(List<Scrip> scrip);
	
}
