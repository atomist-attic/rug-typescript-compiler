package com.atomist.rug.compiler.typescript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class SourceFile {

    private final String filename;
    private final URI uri;
    private final String contents;

    public SourceFile(URI uri, String contents) {
        this.filename = basename(uri.toString());
        this.uri = uri;
        this.contents = contents;
    }

    public static SourceFile fromURL(URL url, Charset cs) throws IOException {
        URI uri;
        try {
            uri = url.toURI();
        }
        catch (URISyntaxException e) {
            throw new IOException("Illegal URI", e);
        }
        try (InputStream is = url.openStream()) {
            return fromStream(is, uri, cs);
        }
    }
    
    public static SourceFile fromStream(InputStream is, URI uri, Charset cs) throws IOException {
        byte[] barr = IOUtils.toByteArray(is);
        String source = new String(barr, cs);
        return new SourceFile(uri, source);
    }

    public String filename() {
        return filename;
    }

    public URI uri() {
        return uri;
    }

    public String contents() {
        return contents;
    }

    private static String basename(String str) {
        int sl = str.lastIndexOf('/');
        if (sl >= 0) {
            return str.substring(sl + 1);
        }
        return str;
    }

    public static SourceFile fromFile(File f, Charset cs) throws IOException {
        try (InputStream is = new FileInputStream(f)) {
          return fromStream(is, f.toURI(), cs);
        }
      }

}
