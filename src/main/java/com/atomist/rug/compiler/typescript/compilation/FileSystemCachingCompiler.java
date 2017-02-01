package com.atomist.rug.compiler.typescript.compilation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.atomist.rug.compiler.typescript.ScriptLoader;
import com.atomist.rug.compiler.typescript.TypeScriptCompilationException;

class FileSystemCachingCompiler implements Compiler {

    private static final String CACHE_DIR = System.getProperty("ts.compilerCache",
            System.getProperty("user.dir") + File.separator + ".jscache");

    private final File cacheDir;
    private final Compiler delegate;

    public FileSystemCachingCompiler(Compiler delegate) {
        this.delegate = delegate;
        this.cacheDir = new File(CACHE_DIR);
    }

    public FileSystemCachingCompiler(Compiler delegate, String path) {
        this.delegate = delegate;
        this.cacheDir = new File(path);
    }

    @Override
    public void compile(String fileName, ScriptLoader scriptLoader) {
        String jsFileName = toJavaScriptName(fileName);
        String contents = scriptLoader.sourceFor(fileName, fileName);
        String hash = calculateHash(contents);
        String hashedFile = hash + ".js";
        String hashedMapFile = hash + ".js.map";

        File cachedFile = new File(cacheDir, hashedFile);
        File cachedMapFile = new File(cacheDir, hashedMapFile);
        
        if (cachedFile.exists() && cachedMapFile.exists()) {
            writeFromCache(jsFileName, scriptLoader, cachedFile);
            writeFromCache(jsFileName + ".map", scriptLoader, cachedMapFile);
        }
        else {
            delegate.compile(fileName, scriptLoader);
            contents = scriptLoader.sourceFor(jsFileName, fileName);
            writeToCache(contents, hashedFile);
            contents = scriptLoader.sourceFor(jsFileName + ".map", fileName);
            writeToCache(contents, hashedMapFile);
        }
    }

    @Override
    public void init() {
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
    }

    @Override
    public void shutdown() {
    }

    private String calculateHash(String data) {
        return DigestUtils.md5Hex(data);
    }

    private String toJavaScriptName(String filename) {
        return filename.replaceAll(".ts$", ".js");
    }

    private void writeFromCache(String filename, ScriptLoader scriptLoader, File cachedFile) {
        try {
            scriptLoader.writeOutput(filename,
                    IOUtils.toString(new FileInputStream(cachedFile), StandardCharsets.ISO_8859_1));
        }
        catch (FileNotFoundException e) {
            throw new TypeScriptCompilationException("Error compiling TypeScript", e);
        }
        catch (IOException e) {
            throw new TypeScriptCompilationException("Error compiling TypeScript", e);
        }
    }

    private void writeToCache(String contents, String hash) {
        try {
            IOUtils.write(contents, new FileOutputStream(new File(cacheDir, hash)),
                    StandardCharsets.ISO_8859_1);
        }
        catch (FileNotFoundException e) {
            throw new TypeScriptCompilationException("Error compiling TypeScript", e);
        }
        catch (IOException e) {
            throw new TypeScriptCompilationException("Error compiling TypeScript", e);
        }
    }

}
