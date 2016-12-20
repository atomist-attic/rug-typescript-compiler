package com.atomist.rug.compiler.typescript;

import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.source.ArtifactSource;
import com.atomist.source.DirectoryArtifact;
import com.atomist.source.FileArtifact;
import com.coveo.nashorn_modules.AbstractFolder;
import com.coveo.nashorn_modules.Folder;
import com.coveo.nashorn_modules.Require;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.io.IOUtils;
import scala.Option;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    /**
     * Alternative JVM based module loader that _only_ loads things available in supplied artifactSource
     * @param artifacts source of js files, such as a Rug archive
     * @return
     */
    public ScriptEngine init(ArtifactSource artifacts){
        NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
        if (engine == null) {
            throw new TypeScriptException("Nashorn ScriptEngine not found");
        }
        return init(engine,artifacts);
    }
    /**
     * Alternative JVM based module loader that _only_ loads things available in supplied artifactSource
     * @param engine nashorn engine
     * @param artifacts source of js files, such as a Rug archive
     * @return
     */
    public ScriptEngine init(NashornScriptEngine engine, ArtifactSource artifacts){
        try{
            Require.enable(engine, new ArtifactSourceBasedFolder(artifacts));
        }catch(Exception e){
            throw new RuntimeException("Unable to set up ArtifactSource based module loader",e);
        }
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

    class ArtifactSourceBasedFolder extends AbstractFolder {

        private ArtifactSource artifacts;

        ArtifactSourceBasedFolder(ArtifactSource artifacts){
            this(artifacts,null, "");
        }
        private ArtifactSourceBasedFolder(ArtifactSource artifacts, Folder parent, String path){
            super(parent, path);
            this.artifacts = artifacts;

        }

        @Override
        public String getFile(String s) {
            Option<FileArtifact> file = artifacts.findFile(s);
            if(!file.isDefined()){
                return null;
            }
            return file.get().content();
        }

        @Override
        public Folder getFolder(String s) {
            Option<DirectoryArtifact> dir = artifacts.findDirectory(s);
            if(!dir.isDefined()){
                return null;
            }
            return new ArtifactSourceBasedFolder(artifacts.underPath(s), this,getPath() + s +  "/");
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
