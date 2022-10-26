package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.JavaTS;

/**
 * A tree node for compound assignment operator.
 *
 * For example:
 * <pre>
 *   <em>variable</em> <em>operator</em> <em>expression</em>
 * </pre>
 *
 * @jls 15.26.2 Compound Assignment Operators
 *
 * @author me
 */

public record CompoundAssignmentTree(ExpressionTree variable, JavaTS operator, ExpressionTree expression) implements ExpressionTree{

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(variable);
        children.add(expression);
        return children;
    }

}
