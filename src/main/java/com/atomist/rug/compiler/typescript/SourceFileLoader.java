package com.atomist.rug.compiler.typescript;

import java.io.IOException;

public interface SourceFileLoader {

  SourceFile getSource(String fileName, String baseFileName) throws IOException;
  
}
