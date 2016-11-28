package com.atomist.rug.compiler.typescript;

@SuppressWarnings("serial")
public class TypeScriptCompilationException extends TypeScriptException {

    public TypeScriptCompilationException(String msg) {
        super(msg);
    }
    
    public TypeScriptCompilationException(String msg, Throwable e) {
        super(msg, e);
    }
}
