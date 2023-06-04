package rm4j.compiler.core;

import java.io.File;
import java.io.IOException;
import rm4j.compiler.tree.CompilationUnitTree;

public class JavaCompiler{

    private static final String BAR = "---------------------------------------------------------------------------------------";

    private final JavaLexer lexer = new JavaLexer();
    private final JavaParser parser = new JavaParser();

    public CompilationUnitTree compile(File file){
        try{
            return parser.parse(file, lexer.run(file));
        }catch (IOException | CompileException e){
            return CompilationUnitTree.ERROR;
        }
    }

}
