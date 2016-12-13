package com.atomist.rug.compiler.typescript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class SourceFile {

    private final String contents;

    private final String filename;

    private final URI uri;

    public SourceFile(URI uri, String contents) {
        filename = basename(uri.toString());
        this.uri = uri;
        this.contents = contents;
    }

    public String contents() {
        return contents;
    }
    
    public String filename() {
        return filename;
    }

    @Override
    public String toString() {
        return contents();
    }
    
    public URI uri() {
        return uri;
    }
    
    public static SourceFile createFrom(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return createFrom(is, file.toURI());
        }
        catch (FileNotFoundException e) {
            throw new TypeScriptException(
                    String.format("File %s not found", file.getAbsolutePath()), e);
        }
        catch (IOException e) {
            throw new TypeScriptException("Error reading stream", e);
        }
    }

    public static SourceFile createFrom(InputStream is, URI uri) {
        try {
            byte[] barr = IOUtils.toByteArray(is);
            String source = new String(barr);
            return new SourceFile(uri, source);
        }
        catch (IOException e) {
            throw new TypeScriptException("Error reading stream", e);
        }
    }

    public static SourceFile createFrom(URL url) {
        URI uri;
        try {
            uri = url.toURI();
        }
        catch (URISyntaxException e) {
            throw new TypeScriptException(String.format("Invalid uri %s provided", url), e);
        }
        try (InputStream is = url.openStream()) {
            return createFrom(is, uri);
        }
        catch (IOException e) {
            throw new TypeScriptException(String.format("Error opening stream from %s", url), e);
        }
    }

    private static String basename(String str) {
        int sl = str.lastIndexOf('/');
        if (sl >= 0) {
            return str.substring(sl + 1);
        }
        return str;
    }
}
