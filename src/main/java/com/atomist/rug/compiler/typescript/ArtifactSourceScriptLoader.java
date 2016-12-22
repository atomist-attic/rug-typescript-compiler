package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;

import scala.Option;

class ArtifactSourceScriptLoader implements ScriptLoader {

    private final ArtifactSource source;

    public ArtifactSourceScriptLoader(ArtifactSource source) {
        this.source = source;
    }

    @Override
    public String sourceFor(String filename, String baseFilename) {
        Option<FileArtifact> file = source.findFile(filename);
        if (file.isDefined()) {
            return file.get().content();
        }
        else if (filename.equals("typescript/lib/lib.es5.d.ts")) {
            // Delegate to resolution from outside the artifact
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(filename)) {
                if (is != null) {
                    return IOUtils.toString(is, Charset.defaultCharset());
                }
            }
            catch (IOException e) {
                throw new TypeScriptCompilationException(
                        String.format("Error occured loading source for %s ", filename), e);
            }
        }
        throw new TypeScriptCompilationException(
                String.format("Source for %s couldn't be found", filename));
    }
}