package com.whiteowl.core.backtest.dsl;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrategyCompiler {

    private static final List<String> BLOCKED_RECEIVERS = List.of(
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.ClassLoader"
    );

    private static final List<String> BLOCKED_STAR_IMPORTS = List.of(
            "java.io",
            "java.net",
            "java.nio",
            "java.lang.reflect"
    );

    public static Script compile(String scriptText) throws StrategyCompilationException {
        CompilerConfiguration config = buildCompilerConfig();
        GroovyShell shell = new GroovyShell(config);
        try {
            return shell.parse(scriptText);
        } catch (MultipleCompilationErrorsException e) {
            throw extractCompilationError(e);
        }
    }

    private static CompilerConfiguration buildCompilerConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(StrategyDsl.class.getName());
        config.addCompilationCustomizers(buildImportCustomizer(), buildSecurityCustomizer());
        return config;
    }

    private static ImportCustomizer buildImportCustomizer() {
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStaticStars("java.lang.Math");
        return imports;
    }

    private static SecureASTCustomizer buildSecurityCustomizer() {
        SecureASTCustomizer secure = new SecureASTCustomizer();
        @SuppressWarnings("rawtypes")
        List<Class> receivers = BLOCKED_RECEIVERS.stream()
                .map(StrategyCompiler::loadClass)
                .map(c -> (Class) c)
                .toList();
        secure.setDisallowedReceiversClasses(receivers);
        secure.setDisallowedStarImports(BLOCKED_STAR_IMPORTS);
        secure.setDisallowedStaticStarImports(BLOCKED_STAR_IMPORTS);
        return secure;
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static StrategyCompilationException extractCompilationError(MultipleCompilationErrorsException e) {
        var errors = e.getErrorCollector().getErrors();
        if (!errors.isEmpty() && errors.get(0) instanceof SyntaxErrorMessage sem) {
            SyntaxException cause = sem.getCause();
            return new StrategyCompilationException(cause.getLine(), cause.getMessage());
        }
        return new StrategyCompilationException(0, e.getMessage());
    }

}
