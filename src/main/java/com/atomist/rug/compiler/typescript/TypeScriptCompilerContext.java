package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;

import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;

public class TypeScriptCompilerContext {
    
    private com.atomist.rug.compiler.typescript.compilation.Compiler compiler;
    private TypeScriptCompiler typeScriptCompiler;

    public ScriptEngine init() {
        ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
        if (engine == null) {
            throw new TypeScriptException("Nashorn ScriptEngine not found");
        }
        return init(engine);
    }

    public ScriptEngine init(ScriptEngine engine) {
        safeEval("exports = {}", engine);

        Bindings bindings = engine.createBindings();
        bindings.put("sourceFileLoader", sourceFileLoader(engine));
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

        safeEval(npmModuleLoader(), engine);
        return engine;
    }
    
    public TypeScriptCompiler compiler() {
        return typeScriptCompiler;
    }
    
    public void shutdown() {
        compiler.shutdown();
    }

    private String npmModuleLoader() {
        try {
            return IOUtils.toString(TypeScriptCompilerContext.class.getResourceAsStream("/utils/jvm-npm.js"),
                    StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            // can't really happen as I put it on the classpath
        }
        return "require = {}";
    }

    private void safeEval(String script, ScriptEngine engine) {
        try {
            engine.eval(script);
        }
        catch (ScriptException e) {
            throw new TypeScriptException("Error evaluating script", e);
        }
    }

    private SourceFileLoader sourceFileLoader(ScriptEngine engine) {
        compiler = CompilerFactory.create();
        typeScriptCompiler = new TypeScriptCompiler(compiler);
        return new DefaultSourceFileLoader(compiler, engine);
    }

}
