package com.whiteowl.core.script;

import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class ScriptClassCache {

    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    public Class<? extends Script> getOrCompile(ScriptDescriptor descriptor,
                                                 ScriptRepository repository,
                                                 Class<? extends Script> baseClass)
            throws IOException, ScriptCompilationException {
        String key = buildKey(descriptor, baseClass);
        long sourceTimestamp = Files.getLastModifiedTime(descriptor.getScriptPath()).toMillis();
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && entry.sourceTimestamp == sourceTimestamp) {
            log.debug("Cache HIT for '{}' [{}]", descriptor.getName(), baseClass.getSimpleName());
            return entry.compiledClass;
        }
        log.debug("Cache MISS for '{}' [{}] — compiling", descriptor.getName(), baseClass.getSimpleName());
        long t0 = System.nanoTime();
        String source = repository.loadScript(descriptor);
        Class<? extends Script> clazz = ScriptCompiler.compileClass(source, baseClass, List.of());
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        log.debug("Compiled '{}' in {}ms", descriptor.getName(), elapsed);
        memoryCache.put(key, new CacheEntry(clazz, sourceTimestamp));
        return clazz;
    }

    public void invalidate(ScriptDescriptor descriptor) {
        memoryCache.keySet().removeIf(k -> k.startsWith(descriptor.getId() + ":"));
    }

    private static String buildKey(ScriptDescriptor descriptor, Class<? extends Script> baseClass) {
        return descriptor.getId() + ":" + baseClass.getSimpleName();
    }

    private record CacheEntry(Class<? extends Script> compiledClass, long sourceTimestamp) {
    }

}
