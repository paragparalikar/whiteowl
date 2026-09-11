package com.whiteowl.workbench.watchlist.repository;

import com.whiteowl.workbench.watchlist.model.Watchlist;

import java.util.List;

public record WatchlistDto(String name, List<String> scripIds) {

    static WatchlistDto fromWatchlist(Watchlist w) {
        return new WatchlistDto(w.getName(), List.copyOf(w.getScripIds()));
    }

    Watchlist toWatchlist() {
        return new Watchlist(name, scripIds != null ? scripIds : List.of());
    }

    static List<WatchlistDto> fromWatchlists(List<Watchlist> watchlists) {
        return watchlists.stream().map(WatchlistDto::fromWatchlist).toList();
    }

    static List<Watchlist> toWatchlists(List<WatchlistDto> dtos) {
        return dtos.stream().map(WatchlistDto::toWatchlist).toList();
    }

}
