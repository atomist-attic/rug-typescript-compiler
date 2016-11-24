package com.atomist.rug.compiler.typescript;

import java.io.InputStream;

public abstract class TypeScriptClasspathResolver {

    public static InputStream resolve(String file, String base) {
        System.out.println(file);
        ClassLoader cls = Thread.currentThread().getContextClassLoader();
        int ix = file.lastIndexOf("node_modules/");
        if (ix > 0) {
            file = file.substring(ix + "node_modules/".length());

            // First try the classpath
            InputStream is = cls.getResourceAsStream(file);
            if (is != null) {
                return is;
            }
        }

        // first segment is the module name which we don't have on the classpath
        ix = file.indexOf('/');
        file = file.substring(ix + 1);
        InputStream is = cls.getResourceAsStream(file);
        if (is != null) {
            return is;
        }

        // TODO CD fix this
        // we might be dealing with scoped packages -> remove first two segments
        // if (filename.startsWith("@")) {
        // filename =
        // }

        return null;
    }

}
