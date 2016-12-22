package com.atomist.rug.compiler.typescript.compilation;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import com.atomist.rug.compiler.typescript.ScriptLoader;
import com.atomist.rug.compiler.typescript.TypeScriptException;

public abstract class AbstractCompiler<T> implements Compiler {

    private static final String TYPESCRIPT_JS = "typescript/lib/typescriptServices.js";
    private static final String COMPILE_JS = "utils/compile.js";

    private T engine;

    protected void loadScript(String name) {
        URL url = getClass().getClassLoader().getResource(name);
        if (url == null) {
            throw new TypeScriptException(String.format("Error loading %s from classpath", name));
        }
        try {
            evalScript(engine, IOUtils.toString(url, Charset.defaultCharset()));
        }
        catch (IOException e) {
            throw new TypeScriptException(String.format("Error loading %s from classpath", name));
        }
    }

    @Override
    public final void init() {
        if (engine == null) {
            engine = createEngine();
            loadScript(TYPESCRIPT_JS);
            loadScript(COMPILE_JS);
            configureEngine(engine);
        }
    }

    @Override
    public String compile(String filename, ScriptLoader sourceFileLoader) {
        return doCompile(engine, filename, sourceFileLoader);
    }

    @Override
    public final void shutdown() {
        doShutdown(engine);
    }

    protected abstract T createEngine();

    protected void configureEngine(T engine) {
    }

    protected abstract void evalScript(T engine, String src);

    protected abstract String doCompile(T engine, String file, ScriptLoader sourceFileLoader);

    protected void doShutdown(T engine) {
    }
}
