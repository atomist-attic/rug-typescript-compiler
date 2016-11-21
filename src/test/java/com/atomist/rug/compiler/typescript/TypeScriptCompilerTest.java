package com.atomist.rug.compiler.typescript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.atomist.rug.compiler.CompilerRegistry;
import com.atomist.rug.compiler.ServiceLoaderCompilerRegistry;
import com.atomist.source.ArtifactSource;
import com.atomist.source.EmptyArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;

import scala.collection.JavaConversions;

public class TypeScriptCompilerTest {

    private String editorTS = "//import Project from \"./Project\";\n" + 
            "//import File from \"./Project\";\n" + 
            "\n" + 
            "/**\n" + 
            "  Simple editor with no parameters\n" + 
            "*/\n" + 
            "class SimpleEditor implements ProjectEditor<Parameters> {\n" + 
            "\n" + 
            "    edit(project: Project, p: Parameters) {\n" + 
            "        p.validate();\n" + 
            "        project.addFile(\"src/from/typescript\", \"Anders Hjelsberg is God\");\n" + 
            "        return `Edited Project now containing ${project.fileCount()} files: \\n`;\n" + 
            "    }\n" + 
            "}";

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
