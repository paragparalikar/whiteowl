package com.whiteowl.scripting.script;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public final class ScriptDescriptor {

    private final String id;
    private final String name;
    private final ScriptType type;
    private final Path scriptPath;

    @Override
    public String toString() {
        return name;
    }

}
