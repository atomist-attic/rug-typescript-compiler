package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.TypeScriptCompilationException;
import com.atomist.rug.compiler.typescript.compilation.V8Compiler.V8CompilerHelper;

public abstract class CompilerFactory {
    
    private static final boolean DISABLE_V8 = (System.getenv("RUG_DISABLE_V8") != null);
    private static final boolean DISABLE_NASHORN = (System.getenv("RUG_DISABLE_NASHORN") != null);
    
    public static Compiler create() {
        return create(false);
    }
    
    public static Compiler create(boolean cache) {
        Compiler compiler;
        if (V8CompilerHelper.IS_ENABLED && !DISABLE_V8) {
            compiler = new V8Compiler();
        }
        else if (NashornCompiler.IS_ENABLED && !DISABLE_NASHORN) {
            compiler = new NashornCompiler();
        }
        else {
            throw new TypeScriptCompilationException("No suitable compiler available");
        }
        compiler.init();
        
        
        if (cache) {
            return cachingCompiler(compiler);
        }
        else {
            return compiler;
        }
    }

    public static Compiler cachingCompiler(Compiler compiler) {
        Compiler cachingCompiler = new FileSystemCachingCompiler(compiler);
        cachingCompiler.init();
        return cachingCompiler;
    }
    
    public static Compiler cachingCompiler(Compiler compiler, String path) {
        Compiler cachingCompiler = new FileSystemCachingCompiler(compiler, path);
        cachingCompiler.init();
        return cachingCompiler;
    }
}
