package com.atomist.rug.compiler.typescript;

import java.net.URI;

import com.atomist.source.ArtifactSource;

class ArtifactSourceSourceFileLoader implements SourceFileLoader {

    private final ArtifactSource source;
    private final SourceFileLoader sourceFileLoader;

    public ArtifactSourceSourceFileLoader(ArtifactSource source, SourceFileLoader sourceFactory) {
        this.source = source;
        this.sourceFileLoader = sourceFactory;
    }

    @Override
    public SourceFile sourceFor(String filename, String baseFilename) {
        if (source.findFile(filename).isDefined()) {
            return new SourceFile(URI.create(filename),
                    source.findFile(filename).get().content());
        }
        else {
            // Delegate to resolution from outside the artifact
            return sourceFileLoader.sourceFor(filename, baseFilename);
        }
    }
}