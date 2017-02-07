package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.ScriptLoader;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.MemoryManager;

public class V8Compiler extends AbstractCompiler<V8> implements Compiler {

    private MemoryManager memoryManager;

    @Override
    protected V8 createEngine() {
        V8 engine = V8.createV8Runtime();
        memoryManager = new MemoryManager(engine);
        return engine;
    }

    @Override
    protected void configureEngine(V8 engine) {
        engine.add("_newline", System.lineSeparator());
        JavaVoidCallback printlnErr = (V8Object receiver,
                V8Array parameters) -> java.lang.System.out.println(parameters.get(0));
        engine.registerJavaMethod(printlnErr, "_println");
    }

    @Override
    protected void doCompile(V8 engine, String file, ScriptLoader sourceFileLoader) {
        JavaCallback sourceFor = (V8Object receiver, V8Array parameters) -> {
            String fileName = parameters.get(0).toString();
            String baseFilename = parameters.get(1).toString();
            return sourceFileLoader.sourceFor(fileName, baseFilename);
        };
        JavaVoidCallback writeOutput = (V8Object receiver, V8Array parameters) -> {
            String fileName = parameters.get(0).toString();
            String content = parameters.get(1).toString();
            sourceFileLoader.writeOutput(fileName, content);
        };

        V8Object scriptLoader = new V8Object(engine);
        scriptLoader.registerJavaMethod(sourceFor, "sourceFor");
        scriptLoader.registerJavaMethod(writeOutput, "writeOutput");

        V8Array args = new V8Array(engine);
        args.push(file);
        args.push(scriptLoader);

        engine.executeStringFunction("compile", args);
    }

    @Override
    protected void evalScript(V8 engine, String src) {
        engine.executeScript(src);
    }

    @Override
    protected void doShutdown(V8 engine) {
        memoryManager.release();
        engine.release();
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
    }
    
    public static class V8CompilerHelper {
        
        public static boolean IS_ENABLED;

        static {
            try {
                Class.forName("com.eclipsesource.v8.V8");
                IS_ENABLED = true;
            }
            catch (Throwable e) {
                IS_ENABLED = false;
            }
        }
    }
}
