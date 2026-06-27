package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.client.kite.model.KiteSymbol;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public final class KiteMapper {

    public static final KiteMapper INSTANCE = new KiteMapper();

    private static final String HOME = System.getProperty("whiteowl.home",
            System.getProperty("user.home") + File.separator + ".whiteowl");
    private static final String SYMBOL_DIR = "kite" + File.separator + "symbols";
    private static final String FILE_EXTENSION = ".csv";
    private static final String HYPHEN = "-";

    private KiteMapper() {}

    public void warmCache() {
        int total = 0;
        for (KiteExchange exchange : KiteExchange.values()) {
            Path path = Paths.get(HOME, SYMBOL_DIR, exchange.name().toLowerCase() + FILE_EXTENSION);
            if (!Files.exists(path)) continue;
            try (var lines = Files.lines(path)) {
                int count = (int) lines
                        .filter(line -> !line.contains(HYPHEN))
                        .map(KiteSymbol::parseCsv)
                        .peek(this::toScrip)
                        .count();
                total += count;
            } catch (Exception e) {
                log.debug("Failed to read cached symbols for {}", exchange.name());
            }
        }
        log.info("Warmed instrument cache with {} symbols from local files", total);
    }

    @Delegate private final KiteExchangeMapper exchangeMapper = new KiteExchangeMapper();
    @Delegate private final KiteIntervalMapper intervalMapper = new KiteIntervalMapper();
    @Delegate private final KiteProductMapper productMapper = new KiteProductMapper();
    @Delegate private final KiteLimitTypeMapper limitTypeMapper = new KiteLimitTypeMapper();
    @Delegate private final KiteOrderSideMapper orderSideMapper = new KiteOrderSideMapper();
    @Delegate private final KiteOrderStatusMapper orderStatusMapper = new KiteOrderStatusMapper();
    @Delegate private final KiteValidityMapper validityMapper = new KiteValidityMapper();
    @Delegate private final KiteVarietyMapper varietyMapper = new KiteVarietyMapper();
    @Delegate private final KiteScripMapper scripMapper = new KiteScripMapper(exchangeMapper);
    @Delegate private final KiteTickMapper tickMapper = new KiteTickMapper(scripMapper);
    @Delegate private final KiteHoldingMapper holdingMapper = new KiteHoldingMapper(scripMapper);
    @Delegate private final KitePositionMapper positionMapper = new KitePositionMapper(scripMapper, productMapper);
    @Delegate private final KiteFundsMapper fundsMapper = new KiteFundsMapper();
    @Delegate private final KiteOrderMapper orderMapper = KiteOrderMapper.builder()
            .scripMapper(scripMapper)
            .exchangeMapper(exchangeMapper)
            .productMapper(productMapper)
            .limitTypeMapper(limitTypeMapper)
            .orderSideMapper(orderSideMapper)
            .orderStatusMapper(orderStatusMapper)
            .validityMapper(validityMapper)
            .varietyMapper(varietyMapper)
            .build();
    @Delegate private final KiteGttMapper gttMapper = new KiteGttMapper(
            scripMapper, exchangeMapper, productMapper, limitTypeMapper, orderSideMapper);

}
