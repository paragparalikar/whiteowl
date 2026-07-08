package com.whiteowl.core.screener;

import com.whiteowl.core.screener.script.GroovyScreen;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;

import java.util.ArrayList;
import java.util.List;

public final class ScreenRegistry {

    private final ScriptRepository screenerScriptRepository;

    public ScreenRegistry(ScriptRepository screenerScriptRepository) {
        this.screenerScriptRepository = screenerScriptRepository;
    }

    public List<Screen> getScreens() {
        List<Screen> all = new ArrayList<>();
        for (ScriptDescriptor descriptor : screenerScriptRepository.findAll()) {
            all.add(new GroovyScreen(descriptor, screenerScriptRepository));
        }
        return all;
    }

}
