package com.atomist.rug.compiler.typescript;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DefaultSourceFileLoader implements SourceFileLoader {

    private Map<String, SourceFile> sourceCache = new HashMap<>();

    @Override
    public SourceFile getSource(String name, String baseFilename) throws IOException {

        // tsc searches for dependencies in node_modules directory.
        // we are remapping those onto the classpath
        InputStream is = TypeScriptClasspathResolver.resolve(name, baseFilename);
        if (is != null) {
            return SourceFile.fromStream(is, URI.create(name), StandardCharsets.UTF_8);
        }

        if (baseFilename != null && (name.startsWith("./") || name.startsWith("../"))) {
            // resolve relative path
            SourceFile baseSource = getSource(baseFilename, null);
            if (baseSource != null) {
                name = baseSource.uri().resolve(name).normalize().toString();
            }
        }

        SourceFile result = sourceCache.get(name);
        if (result == null) {
            // check if we've got a URL
            URL u;
            if (name.matches("^[a-z]+:/.*")) {
                try {
                    u = new URL(name);
                }
                catch (MalformedURLException e) {
                    // no URL, might be a Windows path instead
                    u = null;
                }
            }
            else {
                u = null;
            }

            // search class path
            if (u == null) {
                u = Thread.currentThread().getContextClassLoader().getResource(name);
            }
            if (u != null) {
                result = SourceFile.fromURL(u, StandardCharsets.UTF_8);
            }

            // search file system (will throw if the file could not be found)
            if (result == null) {
                result = SourceFile.fromFile(new File(name), StandardCharsets.UTF_8);
            }

            // at this point 'result' should never be null
            assert result != null;
            sourceCache.put(name, result);
        }

        return result;
    }
}
