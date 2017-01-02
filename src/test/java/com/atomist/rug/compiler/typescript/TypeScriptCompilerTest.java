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
import javax.script.ScriptEngineManager;
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
            + "        let bla = new Test();\n"
            + "        return \"yeah\"\n" + "    }\n" + "}\n" + "";

    @Test
    public void testBrokenCompile() {
        ArtifactSource source = new EmptyArtifactSource("test");
        FileArtifact file = new StringFileArtifact("MyEditor.ts", JavaConversions.asScalaBuffer(
                Arrays.asList(new String[] { ".atomist", "editors" })), brokenEditorTS);
        source = source.plus(file);

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        try {
            ArtifactSource result = compiler.compile(source);
            fail();
        }
        catch (TypeScriptCompilationException e) {
            System.err.println(e.getMessage());
            assertEquals(".atomist/editors/MyEditor.ts(4,23): error TS2304: Cannot find name 'Test'.\n" + 
                    "        let bla = new Test();\n" + 
                    "----------------------^\n",
                    e.getMessage());
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
        assertTrue(result.findFile(".atomist/editors/MyEditor.js").get().content()
                .contains("var SimpleEditor = (function () {"));
    }

    @Test
    public void testCompileUserModel() throws ScriptException {
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(
                        new File("./src/test/resources/my-editor")));

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);

        String jsContents = result.findFile(".atomist/editors/SimpleEditor.js").get().content();
        assertTrue(jsContents.contains("var myeditor = new SimpleEditor();"));
    }

    @Test
    public void testCompileAndRunWithModules() throws Exception {
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(
                        new File("./src/test/resources/licensing-editors")));

        TypeScriptCompiler compiler = new TypeScriptCompiler();
        assertTrue(compiler.supports(source));
        ArtifactSource result = compiler.compile(source);
        String complexJsContents = result.findFile(".atomist/editors/AddLicenseFile.js").get()
                .content();
        assertTrue(complexJsContents.contains("var editor = {"));
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
