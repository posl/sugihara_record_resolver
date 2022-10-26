package rm4j.compiler.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.test.Test;

public class JavaCompiler{

    private static final String BAR = "---------------------------------------------------------------------------------------";

    private final JavaLexer lexer = new JavaLexer();
    private final JavaParser parser = new JavaParser();

    public CompilationUnitTree compile(File file){
        try{
            return parser.parse(file, lexer.run(file));
        }catch (CompileException e){
            return CompilationUnitTree.ERROR;
        }
    }

}
