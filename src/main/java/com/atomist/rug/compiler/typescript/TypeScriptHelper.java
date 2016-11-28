package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;

import com.atomist.rug.compiler.typescript.compilation.V8Compiler;

public abstract class TypeScriptHelper {
    
    public static ScriptEngine createEngine() {
        ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
        if (engine == null) {
            throw new TypeScriptException("Nashorn ScriptEngine not found");
        }
        return prepareEngine(engine);
    }
    
    public static ScriptEngine prepareEngine(ScriptEngine engine) {
        safeEval("exports = {}", engine);
        
        Bindings bindings = engine.createBindings();
        bindings.put("sourceFileLoader", sourceFileLoader());
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        
        safeEval(npmModuleLoader(), engine);
        return engine;
    }
    
    private static String npmModuleLoader() {
        try {
            return IOUtils.toString(TypeScriptHelper.class.getResourceAsStream("/utils/jvm-npm.js"),
                    StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            // can't really happen as I put it on the classpath
        }
        return "require = {}";
    }

    private static void safeEval(String script, ScriptEngine engine) {
        try {
            engine.eval(script);
        }
        catch (ScriptException e) {
            throw new TypeScriptException("Error evaluating script", e);
        }
    }
    
    private static SourceFileLoader sourceFileLoader() {
        return new DefaultSourceFileLoader(new V8Compiler());
    }

}
