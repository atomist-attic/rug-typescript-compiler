package com.atomist.rug.compiler.typescript;

public interface ScriptLoader {

  String sourceFor(String fileName, String baseFileName);
  
}
