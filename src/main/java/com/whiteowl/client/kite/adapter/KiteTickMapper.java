package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.tick.model.Tick;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class KiteTickMapper {

    private final KiteScripMapper scripMapper;

    public Tick toTick(KiteTick kiteTick) {
        if (kiteTick == null) return null;
        Scrip scrip = scripMapper.getScrip(kiteTick.getToken());
        Tick tick = new Tick();
        tick.setScripId(scrip != null ? scrip.getId() : String.valueOf(kiteTick.getToken()));
        tick.setTimestamp(kiteTick.getLastTradedTime());
        tick.setLastTradedPrice(kiteTick.getLastTradedPrice());
        tick.setVolume(kiteTick.getVolumeTradedToday());
        return tick;
    }

}
