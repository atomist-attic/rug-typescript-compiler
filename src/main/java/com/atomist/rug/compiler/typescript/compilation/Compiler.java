package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.ScriptLoader;

public interface Compiler {
    
    void init();

    void compile(String filename, ScriptLoader scriptLoader);
    
    void shutdown();

}