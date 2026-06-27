package com.whiteowl.core.ranker.script;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.script.ScriptCallStack;
import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptCompiler;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.scrip.model.Scrip;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public final class GroovyRanker implements Ranker {

    private final ScriptDescriptor descriptor;
    private final ScriptRepository repository;

    public GroovyRanker(ScriptDescriptor descriptor, ScriptRepository repository) {
        this.descriptor = descriptor;
        this.repository = repository;
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        try {
            RankerDsl dsl = createInstance();
            dsl.bind(bars);
            ScriptCallStack.push(descriptor.getName());
            try {
                Object result = dsl.run();
                return toDouble(result);
            } finally {
                ScriptCallStack.pop();
            }
        } catch (Exception e) {
            log.debug("Ranker script '{}' failed for {}: {}", descriptor.getName(),
                    scrip.getSymbol(), e.getMessage());
            return null;
        }
    }

    public void recompile() throws IOException, ScriptCompilationException {
        repository.getClassCache().invalidate(descriptor);
        repository.getClassCache().getOrCompile(descriptor, repository, RankerDsl.class);
    }

    private RankerDsl createInstance() throws IOException, ScriptCompilationException {
        Class<? extends Script> clazz = repository.getClassCache()
                .getOrCompile(descriptor, repository, RankerDsl.class);
        Script instance = ScriptCompiler.instantiate(clazz);
        return (RankerDsl) instance;
    }

    private static Double toDouble(Object result) {
        if (result == null) return null;
        if (result instanceof Number n) return n.doubleValue();
        return null;
    }

}
