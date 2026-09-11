package com.whiteowl.scripting.ranker;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Ranker {

    String getName();

    Double rank(Scrip scrip, Bars bars);

    default List<RankerSetting> getSettings() {
        return Collections.emptyList();
    }

    default Map<String, Object> getSettingValues() {
        return Collections.emptyMap();
    }

    default void updateSetting(String name, Object value) {
    }

}
