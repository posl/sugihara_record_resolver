package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree node for an assignment expression.
 *
 * For example:
 * <pre>
 *   <em>variable</em> = <em>expression</em>
 * </pre>
 *
 * @jls 15.26.1 Simple Assignment Operator =
 *
 * @author me
 */
public record AssignmentTree(ExpressionTree variable, ExpressionTree expression) implements ExpressionTree{

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(variable);
        children.add(expression);
        return children;
    }

}