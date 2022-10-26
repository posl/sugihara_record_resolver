package rm4j.compiler.file;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rm4j.compiler.resolution.Accessible;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.IdentifierTree;

public class JavaModule implements Accessible{

    private final ExpressionNameTree name;
    private final CompilationUnitTree specification;
    private final Map<ExpressionNameTree, JavaPackage> associated = new ConcurrentHashMap<>();

    public JavaModule(CompilationUnitTree specification){
        this.specification = specification;
        this.name = specification.module().name();
    }

    public void associatePackage(JavaPackage p){
        associated.put(p.qualifiedName(), p);
    }

    public ExpressionNameTree qualifiedName(){
        return name;
    }

    @Override
    public IdentifierTree simpleName(){
        throw new UnsupportedOperationException();
    }
    
}