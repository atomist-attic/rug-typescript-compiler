package com.atomist.rug.compiler.typescript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.atomist.rug.compiler.CompilerRegistry;
import com.atomist.rug.compiler.ServiceLoaderCompilerRegistry;
import com.atomist.rug.kind.core.ProjectMutableView;
import com.atomist.source.ArtifactSource;
import com.atomist.source.EmptyArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import scala.collection.JavaConversions;

public class TypeScriptCompilerTest {

    private String editorTS = "class SimpleEditor  {\n" + "\n" + "    edit() {\n"
            + "        return \"yeah\"\n" + "    }\n" + "}\n" + "";

    @Test
    public void testCompile() {
        ArtifactSource source = new EmptyArtifactSource("test");
        FileArtifact file = new StringFileArtifact("MyEditor.ts", JavaConversions
                .asScalaBuffer(Arrays.asList(new String[] { ".atomist", "editors" })), editorTS);
        source = source.plus(file);

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);
        assertTrue(result.findFile(".atomist/editors/MyEditor.js").isDefined());
    }

    @Test
    public void testCompileUserModel() throws ScriptException {
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(new File("../user-model")));

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);

        String jsContents = result.findFile(".atomist/editors/SimpleEditor.js").get().content();

        ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
        engine.eval("exports = {}");
        engine.eval(jsContents);
        engine.eval("var editor = new SimpleEditor();");
        ScriptObjectMirror editor = (ScriptObjectMirror) engine.getContext()
                .getBindings(ScriptContext.ENGINE_SCOPE).get("editor");

        ProjectMutableView pmv = new ProjectMutableView(new EmptyArtifactSource(""), source);
        String resultString = (String) editor.callMember("edit", pmv, new Object());

        jsContents = result.findFile(".atomist/editors/ConstructedEditor.js").get().content();
        engine = new ScriptEngineManager(null).getEngineByName("nashorn");
        engine.eval("exports = {}");

        try {
            String npmJs = IOUtils.toString(getClass().getResourceAsStream("/jvm-npm.js"),
                    StandardCharsets.UTF_8);
            engine.eval(npmJs);
        }
        catch (IOException e) {
        }

        engine.eval(jsContents);

        engine.eval("var editor = new ConstructedEditor();");
        editor = (ScriptObjectMirror) engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE)
                .get("editor");

        // ProjectMutableView pmv = new ProjectMutableView(new EmptyArtifactSource(""), source);
        // String resultString = (String) editor.callMember("edit", pmv, new Object());
        // System.out.println(resultString + " " +
        // JavaConversions.asJavaCollection(source.allFiles()).size());
    }

    @Test
    public void testCompileThroughCompilerFactory() {
        ArtifactSource source = new EmptyArtifactSource("test");
        FileArtifact file = new StringFileArtifact("MyEditor.ts", JavaConversions
                .asScalaBuffer(Arrays.asList(new String[] { ".atomist", "editors" })), editorTS);
        source = source.plus(file);

        CompilerRegistry registry = new ServiceLoaderCompilerRegistry();
        Collection<com.atomist.rug.compiler.Compiler> compilers = JavaConversions
                .asJavaCollection(registry.findAll(source));
        assertEquals(1, compilers.size());
    }

}
