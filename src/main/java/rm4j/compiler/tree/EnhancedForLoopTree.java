package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree node for an "enhanced"{@code for}loop statement.
 *
 * For example:
 * <pre>
 *   for ( <em>variable</em> : <em>expression</em> )
 *       <em>statement</em>
 * </pre>
 *
 * @jls 14.14.2 The enhanced for statement
 *
 * @author me
 */
public record EnhancedForLoopTree(VariableTree variable, ExpressionTree expression, StatementTree statement) implements StatementTree{

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(3);
        children.add(variable);
        children.add(expression);
        children.add(statement);
        return children;
    }

}