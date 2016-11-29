package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.TypeScriptCompilationException;

public abstract class CompilerFactory {
    
    private static final boolean DISABLE_V8 = (System.getenv("RUG_DISABLE_V8") != null);
    private static final boolean DISABLE_NASHORN = (System.getenv("RUG_DISABLE_NASHORN") != null);
    
    public static Compiler create() {
        Compiler compiler;
        if (V8Compiler.IS_ENABLED && !DISABLE_V8) {
            compiler = new V8Compiler();
        }
        else if (NashornCompiler.IS_ENABLED && !DISABLE_NASHORN) {
            compiler = new NashornCompiler();
        }
        else {
            throw new TypeScriptCompilationException("No suitable compiler available");
        }
        compiler.init();
        return compiler;
    }

}
