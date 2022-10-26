package rm4j.compiler.file;

import java.util.Map;

import rm4j.compiler.resolution.Accessible;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.IdentifierTree;

public class JavaPackage implements Accessible{
    private final ExpressionNameTree name;
    private final Map<IdentifierTree, Accessible> contents;
    
    public JavaPackage(ExpressionNameTree name, Map<IdentifierTree, Accessible> contents){
        this.name = name;
        this.contents = contents;
    }
    
    public ExpressionNameTree qualifiedName(){
        return name;
    }

    public void accept(Map<IdentifierTree, Accessible> contents){
        this.contents.putAll(contents);
    }

    @Override
    public IdentifierTree simpleName(){
        return name.identifier();
    }

}
