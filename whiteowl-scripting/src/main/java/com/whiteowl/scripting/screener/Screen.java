package com.whiteowl.scripting.screener;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.scrip.model.Scrip;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Screen {

    String getName();

    boolean matches(Scrip scrip, Bars bars);

    default List<ScreenSetting> getSettings() {
        return Collections.emptyList();
    }

    default Map<String, Object> getSettingValues() {
        return Collections.emptyMap();
    }

    default void updateSetting(String name, Object value) {
    }

    default String getId() {
        return getClass().getName();
    }

}
