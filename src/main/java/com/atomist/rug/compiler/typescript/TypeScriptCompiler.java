package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.typescript.v8.V8Compiler;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;

import scala.collection.JavaConversions;

public class TypeScriptCompiler implements Compiler {

    private static final Logger logger = LoggerFactory.getLogger(TypeScriptCompiler.class);

    private V8Compiler compiler;

    @Override
    public ArtifactSource compile(ArtifactSource source) {
        if (compiler == null) {
            initCompiler();
        }
        SourceFileLoader sourceFactory = new ArtifactSourceSourceFileLoader(source, compiler);

        List<FileArtifact> files = filterSourceFiles(source);
        List<FileArtifact> compiledFiles = files.stream().map(f -> {
            try {
                String compiled = compiler.compile(f.path(), sourceFactory);
                if (logger.isDebugEnabled()) {
                    logger.debug("Successfully compiled typescript {} to \n{}", f.path(), compiled);
                }
                return new StringFileArtifact(f.name().replace(".ts", ".js"), f.pathElements(),
                        compiled);
            }
            catch (IOException e) {
                // handle exception
                e.printStackTrace();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        ArtifactSource result = source;
        for (FileArtifact compileFile : compiledFiles.stream().filter(c -> c != null)
                .collect(Collectors.toList())) {
            result = result.plus(compileFile);
        }

        return result;
    }

    protected List<FileArtifact> filterSourceFiles(ArtifactSource source) {
        List<FileArtifact> files = JavaConversions.asJavaCollection(source.allFiles()).stream()
                .filter(f -> f.path().startsWith(".atomist/") && f.name().endsWith(".ts"))
                .filter(f -> !f.path().startsWith(".atomist/node_modules/"))
                .collect(Collectors.toList());
        return files;
    }

    @Override
    public boolean supports(ArtifactSource source) {
        return !filterSourceFiles(source).isEmpty();
    }

    private synchronized void initCompiler() {
        if (V8Compiler.supportsV8()) {
            compiler = new V8Compiler();
        }
        else {
            // throw
        }
    }

    private static class ArtifactSourceSourceFileLoader implements SourceFileLoader {

        private final ArtifactSource source;
        private final SourceFileLoader sourceFactory;

        public ArtifactSourceSourceFileLoader(ArtifactSource source, V8Compiler compiler) {
            this.source = source;
            this.sourceFactory = new DefaultSourceFileLoader();
        }

        @Override
        public SourceFile getSource(String filename, String baseFilename) throws IOException {
            if (source.findFile(filename).isDefined()) {
                return new SourceFile(URI.create(filename),
                        source.findFile(filename).get().content());
            }
            else {
                // Delegate to resolution from outside the artifact
                return sourceFactory.getSource(filename, baseFilename);
            }
        }
    }
}
