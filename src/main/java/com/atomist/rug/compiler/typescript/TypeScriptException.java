package com.atomist.rug.compiler.typescript;

@SuppressWarnings("serial")
public class TypeScriptException extends RuntimeException {
    
    public TypeScriptException(String msg) {
        super(msg);
    }
    
    public TypeScriptException(String msg, Throwable e) {
        super(msg, e);
    }
    
}
