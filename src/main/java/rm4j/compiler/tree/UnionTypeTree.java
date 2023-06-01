package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a union type expression in a multicatch
 * variable declaration.
 *
 * @author me
 *
 * @since 1.7
 */

public record UnionTypeTree(ArrayList<TypeTree> typeAlternatives) implements TypeTree{

    static UnionTypeTree parse(JavaTokenManager src) throws CompileException{
        return new UnionTypeTree(Tree.getListWithoutBracket(NameTree::resolveTypeOrName, JavaTS.VERTICAL_BAR, src));
    }

    @Override
    public List<Tree> children(){
        return new ArrayList<>(typeAlternatives);
    }

    @Override
    public String toSource(String indent){
        return Tree.listToSource(typeAlternatives, " | ", indent);
    }

    @Override
    public String toQualifiedTypeName() {
        List<String> exprs = new ArrayList<>();
        String s = "";
        for(var type : typeAlternatives){
            exprs.add(type.toQualifiedTypeName());
        }
        exprs.sort(String.CASE_INSENSITIVE_ORDER);
        for(int i = 0; i < exprs .size(); i++){
            s += exprs.get(i) + ((i == exprs.size() - 1)? "" : " | ");
        }
        return s;
    }


}
