package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for an array type.
 *
 * For example:
 * <pre>
 *   <em>type</em> []
 * </pre>
 *
 * @jls 10.1 Array Types
 *
 * @author me
 */
public record ArrayTypeTree(TypeTree elementType) implements TypeTree{

    static ArrayTypeTree parse(TypeTree elementType, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.LEFT_SQUARE_BRACKET, JavaTS.RIGHT_SQUARE_BRACKET);
        return new ArrayTypeTree(elementType);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(elementType);
        return children;
    }

    @Override
    public String toSource(String indent){
        return elementType.toSource(indent) + "[]";
    }

    @Override
    public String toQualifiedTypeName(){
        return elementType.toQualifiedTypeName() + "[]";
    }

}
