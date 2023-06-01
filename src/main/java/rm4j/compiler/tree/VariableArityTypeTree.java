package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

public record VariableArityTypeTree(TypeTree type) implements TypeTree{

    static VariableArityTypeTree parse(TypeTree elementType, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.ELLIPSIS);
        return new VariableArityTypeTree(elementType);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(type);
        return children;
    }

    @Override
    public String toSource(String indent){
        return type.toSource(indent) + "...";
    }

    @Override
    public String toQualifiedTypeName() {
        return type.toQualifiedTypeName() + "...";
    }

}
