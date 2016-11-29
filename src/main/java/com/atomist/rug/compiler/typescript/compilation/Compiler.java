package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.SourceFileLoader;

public interface Compiler {
    
    void init();

    String compile(String filename, SourceFileLoader sourceFileLoader);
    
    void shutdown();

}