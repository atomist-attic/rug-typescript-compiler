package com.atomist.rug.compiler.typescript.compilation;

import java.util.function.Consumer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.atomist.rug.compiler.typescript.SourceFileLoader;
import com.atomist.rug.compiler.typescript.TypeScriptException;

public class NashornCompiler extends AbstractCompiler<ScriptEngine> implements Compiler {

    public static final boolean IS_ENABLED;

    static {
        IS_ENABLED = new ScriptEngineManager(null).getEngineByName("nashorn") != null;
    }

    @Override
    protected ScriptEngine createEngine() {
        return new ScriptEngineManager(null).getEngineByName("nashorn");
    }

    @Override
    protected void configureEngine(ScriptEngine engine) {
        engine.put("_newline", System.lineSeparator());
        engine.put("_println", (Consumer<Object>) System.out::println);
    }

    @Override
    protected void evalScript(ScriptEngine engine, String src) {
        try {
            engine.eval(src);
        }
        catch (ScriptException e) {
            throw new TypeScriptException("Error evaluating script", e);
        }
    }

    @Override
    protected String doCompile(ScriptEngine engine, String file,
            SourceFileLoader sourceFileLoader) {
        try {
            return (String) ((Invocable) engine).invokeFunction("compile", file, sourceFileLoader);
        }
        catch (NoSuchMethodException e) {
            throw new TypeScriptException(e.getMessage(), e);
        }
        catch (ScriptException e) {
            throw new TypeScriptException(e.getMessage(), e);
        }
    }
}
