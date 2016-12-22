package com.atomist.rug.compiler.typescript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

import com.atomist.rug.compiler.CompilerRegistry;
import com.atomist.rug.compiler.ServiceLoaderCompilerRegistry$;
import com.atomist.source.ArtifactSource;
import com.atomist.source.EmptyArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import scala.collection.JavaConversions;

@SuppressWarnings("restriction")
public class TypeScriptCompilerTest {

    private String editorTS = "class SimpleEditor  {\n" + "\n" + "    edit() {\n"
            + "        return \"yeah\"\n" + "    }\n" + "}\n" + "";

    private String brokenEditorTS = "class SimpleEditor  {\n" + "\n" + "    edit() {\n"
            + "        rurn \"yeah\"\n" + "    }\n" + "}\n" + "";

    @Test
    public void testBrokenCompile() {
        ArtifactSource source = new EmptyArtifactSource("test");
        FileArtifact file = new StringFileArtifact("MyEditor.ts", JavaConversions
                .asScalaBuffer(Arrays.asList(new String[] { ".atomist", "editors" })), brokenEditorTS);
        source = source.plus(file);

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        try {
            ArtifactSource result = compiler.compile(source);
            fail();
        }
        catch (TypeScriptCompilationException e) {
            assertEquals(".atomist/editors/MyEditor.ts(4,14): error TS1005: ';' expected.", e.getMessage());
        }
    }

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
                new SimpleFileSystemArtifactSourceIdentifier(new File("./src/test/resources/my-editor")));

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);
        
        String jsContents = result.findFile(".atomist/editors/SimpleEditor.js").get().content();
        
        ScriptEngine engine = new TypeScriptCompilerContext().init(result.underPath(".atomist"));
        engine.eval(jsContents);
        engine.eval("var editor = new SimpleEditor();");
        ScriptObjectMirror editor = (ScriptObjectMirror) engine.getContext()
                .getBindings(ScriptContext.ENGINE_SCOPE).get("editor");
        jsContents = result.findFile(".atomist/editors/ConstructedEditor.js").get().content();
        engine.eval(jsContents);
        engine.eval("var editor = new ConstructedEditor();");
        editor = (ScriptObjectMirror) engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE)
                .get("editor");
        
    }
    
    @Test
    public void testCompileAndRunWithModules() throws Exception{
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(new File("./src/test/resources/licensing-editors")));

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);
        String complexJsContents = result.findFile(".atomist/editors/AddLicenseFile.js").get().content();
        ScriptEngine engine = new TypeScriptCompilerContext().init(result.underPath(".atomist"));
        engine.eval(complexJsContents);
        assertNotNull(engine.get("editor")); //means we were able to require the atomist nodoe module from the artifact source
    }
    @Test
    public void testCompileThroughCompilerFactory() {
        ArtifactSource source = new EmptyArtifactSource("test");
        FileArtifact file = new StringFileArtifact("MyEditor.ts", JavaConversions
                .asScalaBuffer(Arrays.asList(new String[] { ".atomist", "editors" })), editorTS);
        source = source.plus(file);

        CompilerRegistry registry = ServiceLoaderCompilerRegistry$.MODULE$;
        Collection<com.atomist.rug.compiler.Compiler> compilers = JavaConversions
                .asJavaCollection(registry.findAll(source));
        assertEquals(1, compilers.size());
    }

}
