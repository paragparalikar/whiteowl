package com.whiteowl.core.scrip.repository;

import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;

import java.util.List;
import java.util.Optional;

public interface ScripRepository {

    Optional<Scrip> findById(String id);

    Optional<Scrip> findBySymbolAndExchange(String symbol, Exchange exchange);

    List<Scrip> findByExchange(Exchange exchange);

    List<Scrip> findByScripType(ScripType scripType);

    List<Scrip> findAll();

    List<Scrip> search(String query, int maxResults);

    boolean matches(Scrip scrip, String query);

    void save(Scrip scrip);

    void saveAll(List<Scrip> scrips);

    void deleteById(String id);

}
