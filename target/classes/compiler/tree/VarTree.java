package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

public record VarTree() implements TypeTree{

    static VarTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.VAR);
        return new VarTree();
    }
    
}
