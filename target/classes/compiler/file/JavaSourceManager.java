package rm4j.compiler.file;

import java.io.File;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.resolution.Accessible;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.IdentifierTree;
import rm4j.compiler.tree.StatementTree;

public class JavaSourceManager implements Accessible{

    private final ExpressionNameTree name;
    private final CompilationUnitTree source;

    boolean[] status ={false, false, false, false};

    public JavaSourceManager(ExpressionNameTree name, CompilationUnitTree source){
        this.name = name;
        this.source = source;
        if(source != CompilationUnitTree.ERROR){
            this.status[0] = true;
        }
    }

    public CompilationUnitTree source(){
        return source;
    }

    public File sourceFile(){
        return source.sourceFile();
    }

    public ClassTree topLevelClass() throws CompileException{
        for(StatementTree s : source.typeDecls()){
            if(s instanceof ClassTree c && simpleName().equals(c.simpleName())){
                return c;
            }
        }
        throw new ParserException("Found no class declaration corresponds to the name \"%s\" in %s.".formatted(simpleName().toString(), sourceFile().toString()));
    }

    @Override
    public IdentifierTree simpleName(){
        return name.identifier();
    }
    
}
