package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

public record VoidTree() implements TypeTree{

    static VoidTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.VOID);
        return new VoidTree();
    }
    
    @Override
    public String toSource(String indent){
        return "void";
    }

    @Override
    public String toQualifiedTypeName() {
        return toSource("");
    }

    
}
