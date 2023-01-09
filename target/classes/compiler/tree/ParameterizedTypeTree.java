package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.resolution.Accessor;

/**
 * A tree node for a type expression involving type parameters.
 *
 * For example:
 * <pre>
 *   <em>type</em> &lt; <em>typeArguments</em> &gt;
 * </pre>
 *
 * @jls 4.5.1 Type Arguments of Parameterized Types
 *
 * @author me
 * 
 */
public record ParameterizedTypeTree(Accessor type, ArrayList<TypeTree> typeArguments) implements Accessor, TypeTree{
    
    static final ArrayList<TypeTree> DIAMOND = new ArrayList<>();

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(type);
        children.addAll(typeArguments);
        return children;
    }

    @Override
    public String toSource(String indent){
        String generics = (typeArguments == DIAMOND)? "<>" : "<" + Tree.listToSource(typeArguments, ", ", indent) + ">";
        return type.toSource("") + generics;
    }

}
