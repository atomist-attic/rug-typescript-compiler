package com.atomist.rug.compiler.typescript;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomist.rug.compiler.typescript.compilation.Compiler;

public class DefaultSourceFileLoader implements SourceFileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSourceFileLoader.class);

    private Compiler compiler;
    private ScriptEngine engine;

    private Map<String, SourceFile> cache = new ConcurrentHashMap<>();
    private Map<String, SourceFile> compiledCache = new ConcurrentHashMap<>();
    private Map<String, SourceFile> loadedCache = new ConcurrentHashMap<>();

    public DefaultSourceFileLoader(Compiler compiler) {
        this(compiler, null);
    }

    public DefaultSourceFileLoader(Compiler compiler, ScriptEngine engine) {
        this.compiler = compiler;
        this.engine = engine;
    }

    @Override
    public SourceFile sourceFor(String name, String baseFilename) {
        SourceFile result = doSourceFor(name, baseFilename);

        if (result == null && name.endsWith(".js")) {
            String jsName = name.replace(".js", ".ts");

            if (compiledCache.containsKey(jsName)) {
                result = compiledCache.get(jsName);
            }
            else {
                SourceFile source = doSourceFor(jsName, baseFilename);
                if (source != null) {
                    String compiled = compiler.compile(jsName, this);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Successfully compiled typescript {} to \n{}", name, compiled);
                    }
                    result = SourceFile.createFrom(new ByteArrayInputStream(compiled.getBytes()),
                            URI.create(jsName));
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

        if (name.endsWith(".js") && !loadedCache.containsKey(name)) {
            try {
                if (engine != null && result != null) {
                    engine.eval(result.contents());
                    loadedCache.put(name, result);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Successfully evaluated js for {} in engine", name);
                    }
                }
            }
            catch (ScriptException e) {
                throw new TypeScriptException("Error loading " + name, e);
            }
        }

        return result;
    }

    public InputStream getSourceAsStream(String name, String baseFilename) throws IOException {
        SourceFile source = sourceFor(name, baseFilename);
        if (source != null) {
            return new ByteArrayInputStream(source.contents().getBytes());
        }
        else {
            return null;
        }
    }

    protected SourceFile doSourceFor(String name, String baseFilename) {

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
                        result = SourceFile.createFrom(is, URI.create(name));
                        cache.put(name, result);
                        return result;
                    }
                }
                catch (IOException e) {
                    throw new TypeScriptException(e.getMessage(), e);
                }

            }

            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(".atomist/node_modules/" + file)) {
                if (is != null) {
                    result = SourceFile.createFrom(is, URI.create(name));
                    cache.put(name, result);
                    return result;
                }
            }
            catch (IOException e) {
                throw new TypeScriptException(e.getMessage(), e);
            }

            // First segment is the module name which we don't have on the classpath
            ix = file.indexOf('/');
            file = file.substring(ix + 1);
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(file)) {
                if (is != null) {
                    result = SourceFile.createFrom(is, URI.create(name));
                    cache.put(name, result);
                    return result;
                }
            }
            catch (IOException e) {
                throw new TypeScriptException(e.getMessage(), e);
            }

            if (baseFilename != null && (name.startsWith("./") || name.startsWith("../"))) {
                // resolve relative path
                SourceFile baseSource = sourceFor(baseFilename, null);
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
                result = SourceFile.createFrom(url);
            }

            if (result != null) {
                cache.put(name, result);
            }
        }

        return result;
    }
}
