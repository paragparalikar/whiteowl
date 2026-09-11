package com.whiteowl.client.kite;

import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteTick;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public final class KiteDataPublisherImpl implements KiteDataPublisher {

    private final List<Consumer<KiteTick>> tickConsumers = new CopyOnWriteArrayList<>();
    private final List<Consumer<KiteOrder>> orderConsumers = new CopyOnWriteArrayList<>();

    @Override
    public void publish(KiteTick tick) {
        for (Consumer<KiteTick> consumer : tickConsumers) {
            try {
                consumer.accept(tick);
            } catch (Exception e) {
                log.warn("Error in tick consumer", e);
            }
        }
    }

    @Override
    public void publish(KiteOrder order) {
        for (Consumer<KiteOrder> consumer : orderConsumers) {
            try {
                consumer.accept(order);
            } catch (Exception e) {
                log.warn("Error in order consumer", e);
            }
        }
    }

    public void addTickConsumer(Consumer<KiteTick> consumer) {
        tickConsumers.add(consumer);
    }

    public void removeTickConsumer(Consumer<KiteTick> consumer) {
        tickConsumers.remove(consumer);
    }

    public void addOrderConsumer(Consumer<KiteOrder> consumer) {
        orderConsumers.add(consumer);
    }

    public void removeOrderConsumer(Consumer<KiteOrder> consumer) {
        orderConsumers.remove(consumer);
    }

}
