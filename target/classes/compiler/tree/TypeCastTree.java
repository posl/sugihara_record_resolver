package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree node for a type cast expression.
 *
 * For example:
 * <pre>
 *   ( <em>type</em> ) <em>expression</em>
 * </pre>
 *
 * @jls 15.16 Cast Expressions
 *
 * @author me
 */

public record TypeCastTree(Tree type, ExpressionTree expression) implements ExpressionTree{

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(type);
        children.add(expression);
        return children;
    }
    
}
