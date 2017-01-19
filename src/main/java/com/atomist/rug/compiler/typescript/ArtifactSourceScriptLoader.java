package com.atomist.rug.compiler.typescript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;

import scala.Option;

class ArtifactSourceScriptLoader implements ScriptLoader {

    private ArtifactSource source;

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

    @Override
    public void writeOutput(String fileName, String content) {
        Option<FileArtifact> existing = source.findFile(fileName);
        if (existing.isEmpty()) {
            add(fileName, content);
        }
        else {
            String existingContent = existing.get().content();
            if (!content.equals(existingContent)) {
                add(fileName, existingContent);
            }
        }
    }

    private void add(String fileName, String content) {
        FileArtifact output = StringFileArtifact.apply(fileName, content);
        this.source = source.plus(output);
    }

    public ArtifactSource result() {
        return this.source;
    }
}