package com.whiteowl.client.kite.symbol;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import static com.whiteowl.client.kite.KiteConstant.URL_INSTRUMENTS;

@Slf4j
public final class KiteHttpScripLoader implements KiteScripLoader {

    @Override
    @SneakyThrows
    public List<KiteSymbol> loadByExchange(KiteExchange exchange) {
        InputStream stream = new URI(URL_INSTRUMENTS + exchange.name()).toURL().openStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            List<KiteSymbol> symbols = reader.lines()
                    .skip(1)
                    .map(KiteSymbol::parseCsv)
                    .distinct()
                    .toList();
            log.info("Downloaded {} kite symbols for {}", symbols.size(), exchange.name());
            return symbols;
        }
    }

}
