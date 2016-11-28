package com.atomist.rug.compiler.typescript.compilation;

import java.net.URL;

import com.atomist.rug.compiler.typescript.SourceFile;
import com.atomist.rug.compiler.typescript.SourceFileLoader;
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
        evalScript(engine, SourceFile.createFrom(url).contents());
    }

    @Override
    public final String compile(String file, SourceFileLoader sourceFileLoader) {
        if (engine == null) {
            engine = createEngine();
            loadScript(TYPESCRIPT_JS);
            loadScript(COMPILE_JS);
            configureEngine(engine);
        }
        return doCompile(engine, file, sourceFileLoader);
    }

    protected void configureEngine(T engine) {
        
    }

    protected abstract T createEngine();

    protected abstract void evalScript(T engine, String src);

    protected abstract String doCompile(T engine, String file, SourceFileLoader sourceFileLoader);

}
