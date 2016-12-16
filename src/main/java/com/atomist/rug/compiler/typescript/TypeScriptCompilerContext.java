package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;

import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.source.FileArtifact;

public class TypeScriptCompilerContext {
    
    private com.atomist.rug.compiler.typescript.compilation.Compiler compiler;
    private TypeScriptCompiler typeScriptCompiler;
    private DefaultSourceFileLoader sourceFileLoader;

    public ScriptEngine init() {
        ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
        if (engine == null) {
            throw new TypeScriptException("Nashorn ScriptEngine not found");
        }
        return init(engine);
    }

    public ScriptEngine init(ScriptEngine engine) {
        // Needed for compilation
        safeEval("exports = {}", engine);
        
        // atomist might not be available during compilation
        if (engine.get("atomist") == null) {
            safeEval("atomist = {}", engine);
        }
        
        // Set up npm module loading
        engine.put("sourceFileLoader", sourceFileLoader(engine));
        safeEval(npmModuleLoader(), engine);
        
        return engine;
    }
    
    public void eval(FileArtifact file, ScriptEngine engine) throws ScriptException {
        try {
            sourceFileLoader.push(file);
            engine.eval(file.content());
        }
        finally {
            sourceFileLoader.pop();
        }
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
        sourceFileLoader = new DefaultSourceFileLoader(compiler, engine);
        return sourceFileLoader;
    }

}
