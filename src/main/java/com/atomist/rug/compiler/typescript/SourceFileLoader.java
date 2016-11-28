package com.atomist.rug.compiler.typescript;

public interface SourceFileLoader {

  SourceFile sourceFor(String fileName, String baseFileName);
  
}
