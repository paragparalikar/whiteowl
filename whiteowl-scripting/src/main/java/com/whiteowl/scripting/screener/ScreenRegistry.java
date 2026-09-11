package com.whiteowl.scripting.screener;

import com.whiteowl.scripting.screener.script.GroovyScreen;
import com.whiteowl.scripting.script.ScriptDescriptor;
import com.whiteowl.scripting.script.ScriptRepository;

import java.util.ArrayList;
import java.util.List;

public final class ScreenRegistry {

    private final ScriptRepository screenerScriptRepository;
    private final List<Screen> builtInScreens;

    public ScreenRegistry(ScriptRepository screenerScriptRepository) {
        this(screenerScriptRepository, List.of());
    }

    public ScreenRegistry(ScriptRepository screenerScriptRepository, List<Screen> builtInScreens) {
        this.screenerScriptRepository = screenerScriptRepository;
        this.builtInScreens = builtInScreens;
    }

    public List<Screen> getScreens() {
        List<Screen> all = new ArrayList<>(builtInScreens);
        for (ScriptDescriptor descriptor : screenerScriptRepository.findAll()) {
            all.add(new GroovyScreen(descriptor, screenerScriptRepository));
        }
        return all;
    }

}
