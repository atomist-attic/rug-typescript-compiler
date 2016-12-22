package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;

import com.atomist.source.ArtifactSource;
import com.atomist.source.DirectoryArtifact;
import com.atomist.source.FileArtifact;
import com.coveo.nashorn_modules.AbstractFolder;
import com.coveo.nashorn_modules.Folder;
import com.coveo.nashorn_modules.Require;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import scala.Option;

public class TypeScriptCompilerContext {

    private com.atomist.rug.compiler.typescript.compilation.Compiler compiler;
    private TypeScriptCompiler typeScriptCompiler;

    public TypeScriptCompiler compiler() {
        return typeScriptCompiler;
    }

    public void eval(FileArtifact file, ScriptEngine engine) throws ScriptException {
        engine.eval(file.content());
    }

    /**
     * Alternative JVM based module loader that _only_ loads things available in supplied
     * artifactSource
     * @param artifacts source of js files, such as a Rug archive
     * @return
     */
    public ScriptEngine init(ArtifactSource artifacts) {
        NashornScriptEngine engine = (NashornScriptEngine) new NashornScriptEngineFactory()
                .getScriptEngine();
        if (engine == null) {
            throw new TypeScriptException("Nashorn ScriptEngine not found");
        }
        return init(engine, artifacts);
    }

    /**
     * Alternative JVM based module loader that _only_ loads things available in supplied
     * artifactSource
     * @param engine nashorn engine
     * @param artifacts source of js files, such as a Rug archive
     * @return
     */
    public ScriptEngine init(NashornScriptEngine engine, ArtifactSource artifacts) {
        try {
            Require.enable(engine, new ArtifactSourceBasedFolder(artifacts));
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to set up ArtifactSource based module loader", e);
        }
        return engine;
    }

    public void shutdown() {
        compiler.shutdown();
    }

    private void safeEval(String script, ScriptEngine engine) {
        try {
            engine.eval(script);
        }
        catch (ScriptException e) {
            throw new TypeScriptException("Error evaluating script", e);
        }
    }

    class ArtifactSourceBasedFolder extends AbstractFolder {

        private ArtifactSource artifacts;

        private ArtifactSourceBasedFolder(ArtifactSource artifacts, Folder parent, String path) {
            super(parent, path);
            this.artifacts = artifacts;

        }

        ArtifactSourceBasedFolder(ArtifactSource artifacts) {
            this(artifacts, null, "");
        }

        @Override
        public String getFile(String s) {
            Option<FileArtifact> file = artifacts.findFile(s);
            if (!file.isDefined()) {
                return null;
            }
            return file.get().content();
        }

        @Override
        public Folder getFolder(String s) {
            Option<DirectoryArtifact> dir = artifacts.findDirectory(s);
            if (!dir.isDefined()) {
                return null;
            }
            return new ArtifactSourceBasedFolder(artifacts.underPath(s), this, getPath() + s + "/");
        }
    }
}
