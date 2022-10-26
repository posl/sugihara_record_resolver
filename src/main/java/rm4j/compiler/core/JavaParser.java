package rm4j.compiler.core;

import java.io.File;

import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.JavaTokenManager;

public class JavaParser{

    public CompilationUnitTree parse(File file, TokenList l) throws CompileException{
        JavaTokenManager src = new JavaTokenManager(l);
        try{
            return CompilationUnitTree.parse(file, src);
        }catch (ParserException e){
            throw e;
        }
    }

}
