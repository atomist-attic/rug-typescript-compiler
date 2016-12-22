package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.ScriptLoader;

public interface Compiler {
    
    void init();

    String compile(String filename, ScriptLoader sourceFileLoader);
    
    void shutdown();

}