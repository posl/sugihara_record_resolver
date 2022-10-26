package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.TypeIdentifier;

/**
 * A tree node for a type parameter.
 *
 * For example:
 * <pre>
 *   <em>name</em>
 *
 *   <em>name</em> implements <em>bounds</em>
 *
 *   <em>annotations</em> <em>name</em>
 * </pre>
 *
 * @jls 4.4 Type Variables
 *
 * @author me
 * 
 */

public record TypeParameterTree(ArrayList<AnnotationTree> annotations, IdentifierTree name, ArrayList<TypeTree> bounds)implements TypeIdentifier{

    static TypeParameterTree parse(JavaTokenManager src) throws CompileException{
        ArrayList<AnnotationTree> annotations = Tree.resolveAnnotations(src);
        IdentifierTree name = IdentifierTree.parse(src);
        ArrayList<TypeTree> bounds;
        if(src.match(JavaTS.EXTENDS)){
            src.skip(JavaTS.EXTENDS);
            bounds = Tree.getListWithoutBracket(NameTree::resolveNonArrayTypeOrName, JavaTS.AND, src);
        }else{
            bounds = new ArrayList<>();
        }
        return new TypeParameterTree(annotations, name, bounds);
    }

    @Override
    public IdentifierTree simpleName(){
        return name;
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(annotations);
        children.add(name);
        children.addAll(bounds);
        return children;
    }

}
