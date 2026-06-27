package com.whiteowl.core.script;

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
public final class ScriptCompiler {

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

    public static Script compile(String scriptText, Class<? extends Script> baseClass)
            throws ScriptCompilationException {
        return compile(scriptText, baseClass, List.of());
    }

    public static Script compile(String scriptText, Class<? extends Script> baseClass,
                                  List<String> additionalImports) throws ScriptCompilationException {
        Class<? extends Script> clazz = compileClass(scriptText, baseClass, additionalImports);
        return instantiate(clazz);
    }

    public static Class<? extends Script> compileClass(String scriptText, Class<? extends Script> baseClass,
                                                        List<String> additionalImports)
            throws ScriptCompilationException {
        CompilerConfiguration config = buildCompilerConfig(baseClass, additionalImports);
        GroovyShell shell = new GroovyShell(config);
        try {
            Script script = shell.parse(scriptText);
            @SuppressWarnings("unchecked")
            Class<? extends Script> clazz = (Class<? extends Script>) script.getClass();
            return clazz;
        } catch (MultipleCompilationErrorsException e) {
            throw extractCompilationError(e);
        }
    }

    public static Script instantiate(Class<? extends Script> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate script", e);
        }
    }

    private static CompilerConfiguration buildCompilerConfig(Class<? extends Script> baseClass,
                                                              List<String> additionalImports) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(baseClass.getName());
        config.addCompilationCustomizers(
                buildImportCustomizer(additionalImports),
                buildSecurityCustomizer());
        return config;
    }

    private static ImportCustomizer buildImportCustomizer(List<String> additionalImports) {
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStaticStars("java.lang.Math");
        if (!additionalImports.isEmpty()) {
            imports.addImports(additionalImports.toArray(String[]::new));
        }
        return imports;
    }

    private static SecureASTCustomizer buildSecurityCustomizer() {
        SecureASTCustomizer secure = new SecureASTCustomizer();
        @SuppressWarnings("rawtypes")
        List<Class> receivers = BLOCKED_RECEIVERS.stream()
                .map(ScriptCompiler::loadClass)
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

    private static ScriptCompilationException extractCompilationError(MultipleCompilationErrorsException e) {
        var errors = e.getErrorCollector().getErrors();
        if (!errors.isEmpty() && errors.get(0) instanceof SyntaxErrorMessage sem) {
            SyntaxException cause = sem.getCause();
            return new ScriptCompilationException(cause.getLine(), cause.getMessage());
        }
        return new ScriptCompilationException(0, e.getMessage());
    }

}
