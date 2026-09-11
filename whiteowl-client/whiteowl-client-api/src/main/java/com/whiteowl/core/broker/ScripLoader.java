package com.whiteowl.core.broker;

import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.List;

public interface ScripLoader {

    List<Scrip> loadByExchange(Exchange exchange);

    List<Scrip> loadByExchange(Exchange exchange, boolean forceDownload);

}
