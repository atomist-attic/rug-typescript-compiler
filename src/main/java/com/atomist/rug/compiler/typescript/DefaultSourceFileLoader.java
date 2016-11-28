package com.atomist.rug.compiler.typescript;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomist.rug.compiler.typescript.v8.V8Compiler;

public class DefaultSourceFileLoader implements SourceFileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSourceFileLoader.class);

    private V8Compiler compiler;
    private Map<String, SourceFile> cache = new ConcurrentHashMap<>();
    private Map<String, SourceFile> compiledCache = new ConcurrentHashMap<>();

    public DefaultSourceFileLoader(V8Compiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public SourceFile getSource(String name, String baseFilename) throws IOException {
        SourceFile result = doGetSource(name, baseFilename);
        if (result == null && name.endsWith(".js")) {
            String jsName = name.replace(".js", ".ts");

            if (compiledCache.containsKey(jsName)) {
                result = compiledCache.get(jsName);
            }
            else {
                SourceFile source = doGetSource(jsName, baseFilename);
                if (source != null) {
                    String compiled = compiler.compile(jsName, this);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Successfully compiled typescript {} to \n{}", name, compiled);
                    }
                    result = SourceFile.fromStream(new ByteArrayInputStream(compiled.getBytes()),
                            URI.create(jsName), StandardCharsets.UTF_8);
                    compiledCache.put(jsName, result);
                }
            }
        }

        if (result != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Resolved {} from {} to {}", name, baseFilename, result.uri());
            }
        }
        else {
            LOGGER.warn("Failed to resolve {} from {}", name, baseFilename);
        }

        return result;
    }

    public InputStream getSourceAsStream(String name, String baseFilename) throws IOException {
        SourceFile source = getSource(name, baseFilename);
        if (source == null) {
            return null;
        }
        else {
            return new ByteArrayInputStream(source.contents().getBytes());
        }
    }

    protected SourceFile doGetSource(String name, String baseFilename) throws IOException {
        SourceFile result = cache.get(name);
        if (result == null) {
            // tsc searches for dependencies in node_modules directory.
            // We are remapping those onto the classpath
            String file = name;
            int ix = file.lastIndexOf("node_modules/");
            if (ix > 0) {
                file = file.substring(ix + "node_modules/".length());

                // First try the classpath with normalized name
                try (InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(file)) {
                    if (is != null) {
                        result = SourceFile.fromStream(is, URI.create(name), StandardCharsets.UTF_8);
                        cache.put(name, result);
                        return result;
                    }
                }
            }

            // First segment is the module name which we don't have on the classpath
            ix = file.indexOf('/');
            file = file.substring(ix + 1);
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(file)) {
                if (is != null) {
                    result = SourceFile.fromStream(is, URI.create(name), StandardCharsets.UTF_8);
                    cache.put(name, result);
                    return result;
                }
            }

            if (baseFilename != null && (name.startsWith("./") || name.startsWith("../"))) {
                // resolve relative path
                SourceFile baseSource = getSource(baseFilename, null);
                if (baseSource != null) {
                    name = baseSource.uri().resolve(name).normalize().toString();
                }
            }

            // Require on urls is possible
            URL url;
            if (name.matches("^[a-z]+:/.*")) {
                try {
                    url = new URL(name);
                }
                catch (MalformedURLException e) {
                    // no URL, might be a Windows path instead
                    url = null;
                }
            }
            else {
                url = null;
            }

            // Search the class path again
            if (url == null) {
                url = Thread.currentThread().getContextClassLoader().getResource(name);
            }
            if (url != null) {
                result = SourceFile.fromURL(url, StandardCharsets.UTF_8);
            }

            if (result != null) {
                cache.put(name, result);
            }
        }

        return result;
    }
}
