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

    @Override
    public String toSource(String indent){
        return Tree.listToSource(bounds, " & ", indent);
    }

    @Override
    public String toQualifiedTypeName(){
        List<String> exprs = new ArrayList<>();
        String s = "";
        for(var bound : bounds){
            exprs.add(bound.toQualifiedTypeName());
        }
        exprs.sort(String.CASE_INSENSITIVE_ORDER);
        for(int i = 0; i < exprs .size(); i++){
            s += exprs.get(i) + ((i == exprs.size() - 1)? "" : " & ");
        }
        return s;
    }
}
