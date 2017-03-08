package com.atomist.rug.compiler.typescript;

import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Deltas;
import com.atomist.source.FileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.FileSystemArtifactSourceIdentifier$;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static scala.collection.JavaConversions.asJavaCollection;

public class TypeScriptCompiler implements Compiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptCompiler.class);

    private com.atomist.rug.compiler.typescript.compilation.Compiler compiler;
    
    private boolean externalLifeCycle = false;
    
    public TypeScriptCompiler() {
    }
    
    public TypeScriptCompiler(com.atomist.rug.compiler.typescript.compilation.Compiler compiler) {
        this.externalLifeCycle = true;
        this.compiler = compiler;
    }

    @Override
    public ArtifactSource compile(ArtifactSource source) {
        try {
            ArtifactSourceScriptLoader scriptLoader = new ArtifactSourceScriptLoader(source);

            // Get source files to compile
            List<FileArtifact> files = filterSourceFiles(source);

            if (files.size() > 0) {
                // Init the compiler
                initCompiler();
                
                // Actually compile the files now
                compileFiles(source, scriptLoader, files);
                
                ArtifactSource result = scriptLoader.result();
                Deltas deltas = result.deltaFrom(source);
                if (LOGGER.isDebugEnabled()) {

                    asJavaCollection(deltas.deltas()).forEach(d -> {
                        LOGGER.debug("Successfully compiled TypeScript to file {}, content:\n{}", d.path(),
                                result.findFile(d.path()).get().content());
                    });

                }

                return result;
            }
            else {
                return source;
            }
        }
        finally {
            shutDownCompiler();
        }
    }

    @Override
    public Set<String> extensions() {
        return Collections.singleton("ts");
    }

    @Override
    public String name() {
        return "TypeScript Compiler";
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public boolean supports(ArtifactSource source) {
        return !filterSourceFiles(source).isEmpty();
    }

    private void compileFiles(ArtifactSource source, ScriptLoader scriptLoader,
            List<FileArtifact> files) {
        files.stream().forEach(f -> {
            try {
                compiler.compile(f.path(), scriptLoader);
            }
            catch (Exception e) {
                handleException(e, source);
            }
        });
    }

    private void handleException(Exception e, ArtifactSource source) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("<#>")) {
            int start = msg.indexOf("<#>") + 3;
            int end = msg.lastIndexOf("<#>");
            throw new TypeScriptDetailedCompilationException(msg.substring(start, end).trim(),
                    source);
        }
        else {
            throw new TypeScriptCompilationException("Compilation failed", e);
        }
    }

    private synchronized void initCompiler() {
        if (compiler == null) {
            compiler = CompilerFactory.create();
        }
    }

    private synchronized void shutDownCompiler() {
        if (compiler != null && !externalLifeCycle) {
            compiler.shutdown();
            compiler = null;
        }
    }

    protected List<FileArtifact> filterSourceFiles(ArtifactSource source) {
        return asJavaCollection(source.allFiles()).stream()
                .filter(f -> f.path().startsWith(".atomist/") && f.name().endsWith(".ts"))
                .filter(f -> !f.path().startsWith(".atomist/node_modules/")).collect(toList());
    }

    public static void main(String[] args) throws Exception {
        if(args == null || args.length != 2){
            System.out.println("Usage: TypeScriptCompiler <input-path> <output-path>");
            System.exit(1);
        }
        File inputFile = new File(args[0]);
        if(!inputFile.canRead()){
            throw new IllegalArgumentException("Cannot read input: " + args[0]);
        }

        File outputFile = new File(args[1]);
        outputFile.mkdirs();

        FileSystemArtifactSource input = new FileSystemArtifactSource(FileSystemArtifactSourceIdentifier$.MODULE$.apply(inputFile));


        ArtifactSource outputMem  = new TypeScriptCompiler().compile(input);

        asJavaCollection(outputMem.allFiles()).forEach(f -> {
            PrintWriter pw = null;
            File output = new File(outputFile, f.path());
            try {
                output.getParentFile().mkdirs();
                pw = new PrintWriter(output);
                pw.write(f.content());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(("Could not find file: " + output.getAbsolutePath()),e);
            }finally {
                IOUtils.closeQuietly(pw);
            }
        });

    }
}
