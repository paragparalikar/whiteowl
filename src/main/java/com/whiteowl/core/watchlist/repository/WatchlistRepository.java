package com.whiteowl.core.watchlist.repository;

import com.whiteowl.core.watchlist.model.Watchlist;

import java.util.List;

public interface WatchlistRepository {

    List<Watchlist> loadAll();

    void saveAll(List<Watchlist> watchlists);

}
