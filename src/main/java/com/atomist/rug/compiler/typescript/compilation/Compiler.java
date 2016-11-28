package com.atomist.rug.compiler.typescript.compilation;

import com.atomist.rug.compiler.typescript.SourceFileLoader;

public interface Compiler {

    String compile(String filename, SourceFileLoader sourceFileLoader);

}