package com.whiteowl.workbench.watchlist.repository;

import com.whiteowl.workbench.watchlist.model.Watchlist;

import java.util.List;

public interface WatchlistRepository {

    List<Watchlist> loadAll();

    void saveAll(List<Watchlist> watchlists);

}
