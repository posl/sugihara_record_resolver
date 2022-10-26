package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree node for a basic{@code for}loop statement.
 *
 * For example:
 * <pre>
 *   for ( <em>initializer</em> ; <em>condition</em> ; <em>update</em> )
 *       <em>statement</em>
 * </pre>
 *
 * @jls 14.14.1 The basic for Statement
 *
 * @author me
 */
public record ForLoopTree(ArrayList<StatementTree> initializer, ExpressionTree condition, ArrayList<ExpressionStatementTree> update, StatementTree statement) implements StatementTree{

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(initializer);
        if(condition != null){
            children.add(condition);
        }
        children.addAll(update);
        children.add(statement);
        return children;
    }
    
}
