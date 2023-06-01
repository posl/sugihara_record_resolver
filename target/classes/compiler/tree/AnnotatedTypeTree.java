package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.resolution.Accessor;

/**
 * A tree node for an annotated type.
 *
 * For example:
 * <pre>
 *   {@code @}<em>annotationType String</em>
 *   {@code @}<em>annotationType</em> ( <em>arguments</em> ) <em>Date</em>
 * </pre>
 *
 * @see "JSR 308: Annotations on Java Types"
 *
 * @author me
 */

public record AnnotatedTypeTree(ArrayList<AnnotationTree> annotations, TypeTree type) implements ExpressionTree, Accessor, TypeTree{
    
    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(annotations);
        children.add(type);
        return children;
    }

    @Override
    public String toSource(String indent){
        String s = Tree.listToSource(annotations, " ", indent);
        if(type instanceof ArrayTypeTree a){
            return a.elementType().toSource(indent) + " " + s + "[]";
        }
        return  s + " " + type.toSource(indent);
    }

    @Override
    public String toQualifiedTypeName(){
        return type.toQualifiedTypeName();
    }

}
