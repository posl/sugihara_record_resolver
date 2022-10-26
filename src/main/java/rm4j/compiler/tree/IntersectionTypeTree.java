package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for an intersection type in a cast expression.
 *
 * @author me
 */

public record IntersectionTypeTree(ArrayList<TypeTree> bounds) implements TypeTree{

    static IntersectionTypeTree parse(JavaTokenManager src) throws CompileException{
        return new IntersectionTypeTree(Tree.getListWithoutBracket(NameTree::resolveTypeOrName, JavaTS.AND, src));
    }

    @Override
    public List<Tree> children(){
        return new ArrayList<>(bounds);
    }

}
