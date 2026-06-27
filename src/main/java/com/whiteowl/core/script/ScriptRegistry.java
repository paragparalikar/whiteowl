package com.whiteowl.core.script;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ScriptRegistry {

    private final Map<ScriptType, ScriptRepository> repositories = new EnumMap<>(ScriptType.class);

    public void register(ScriptType type, ScriptRepository repository) {
        repositories.put(type, repository);
    }

    public Optional<ScriptRepository> getRepository(ScriptType type) {
        return Optional.ofNullable(repositories.get(type));
    }

    public Optional<ScriptDescriptor> findScript(ScriptType type, String name) {
        return getRepository(type)
                .map(ScriptRepository::findAll)
                .flatMap(list -> list.stream()
                        .filter(d -> d.getName().equals(name))
                        .findFirst());
    }

}
