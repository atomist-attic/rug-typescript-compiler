package com.atomist.rug.compiler.typescript.v8;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.atomist.rug.compiler.typescript.SourceFile;
import com.atomist.rug.compiler.typescript.SourceFileLoader;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class V8Compiler {
        
    private static final String TYPESCRIPT_JS = "typescript/lib/typescriptServices.js";
    
    private static final String COMPILE_JS = "utils/compile.js";

    private static final String FILENOTFOUNDEXCEPTION = "__UNCHECKEDFILENOTFOUNDEXCEPTION";

    /**
     * The V8 runtime hosting the TypeScript compiler
     */
    private V8 runtime;

    /**
     * Create a V8 runtime that hosts the TypeScript compiler. Load the
     * compiler and a helper script and evaluates them within the runtime.
     * @return the runtime
     */
    private V8 getRuntime() {
        if (runtime == null) {
            // create runtime
            runtime = V8.createV8Runtime();

            // load TypeScript compiler and helper script
            loadScript(TYPESCRIPT_JS);
            loadScript(COMPILE_JS);

            // define some globals
            runtime.add("__lineSeparator", System.lineSeparator());
            JavaCallback isFileNotFoundException = (V8Object receiver, V8Array parameters) -> {
                Object e = parameters.get(0);
                return e instanceof FileNotFoundException
                        || e instanceof RuntimeFileNotFoundException
                        || (e != null && String.valueOf(e).equals(FILENOTFOUNDEXCEPTION));
            };
            runtime.registerJavaMethod(isFileNotFoundException, "__isFileNotFoundException");
            JavaVoidCallback printlnErr = (V8Object receiver,
                    V8Array parameters) -> java.lang.System.err.println(parameters.get(0));
            runtime.registerJavaMethod(printlnErr, "__printlnErr");
        }
        return runtime;
    }

    private void loadScript(String name) {
        URL url = getClass().getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Cannot find " + name + " on classpath");
        }

        try {
            String src = SourceFile.fromURL(url, StandardCharsets.UTF_8).contents();
            runtime.executeScript(src);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not evaluate " + name, e);
        }
    }

    public String compile(String filename, SourceFileLoader sourceFileLoader) throws IOException {
        JavaCallback getSource = (V8Object receiver, V8Array parameters) -> {
            String sourceFilename = parameters.get(0).toString();
            String baseFilename = parameters.get(1).toString();
            try {
                return sourceFileLoader.getSource(sourceFilename, baseFilename).contents();
            }
            catch (FileNotFoundException e) {
                throw new RuntimeFileNotFoundException();
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };

        V8 runtime = getRuntime();
        V8Object v8sourceFactory = new V8Object(runtime);
        v8sourceFactory.registerJavaMethod(getSource, "getSource");

        V8Array args = new V8Array(runtime);
        args.push(filename);
        args.push(v8sourceFactory);

        try {
            return runtime.executeStringFunction("compileTypescript", args);
        }
        finally {
            args.release();
            v8sourceFactory.release();
        }
    }

    public static boolean supportsV8() {
        try {
            Class.forName("com.eclipsesource.v8.V8");
            return true;
        }
        catch (Throwable e) {
            return false;
        }
    }
    
    @SuppressWarnings("serial")
    private static class RuntimeFileNotFoundException extends RuntimeException {
        public RuntimeFileNotFoundException() {
            super(FILENOTFOUNDEXCEPTION);
        }
    }

}
