package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.net.URI;

import com.atomist.source.ArtifactSource;

class ArtifactSourceSourceFileLoader implements SourceFileLoader {

    private final ArtifactSource source;
    private final SourceFileLoader sourceFactory;

    public ArtifactSourceSourceFileLoader(ArtifactSource source, SourceFileLoader sourceFactory) {
        this.source = source;
        this.sourceFactory = sourceFactory;
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