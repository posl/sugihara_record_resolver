package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for an{@code if}statement.
 *
 * For example:
 * <pre>
 *   if ( <em>condition</em> )
 *      <em>thenStatement</em>
 *
 *   if ( <em>condition</em> )
 *       <em>thenStatement</em>
 *   else
 *       <em>elseStatement</em>
 * </pre>
 *
 * @jls 14.9 The if Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record IfTree(ExpressionTree condition, StatementTree thenStatement, StatementTree elseStatement) implements StatementTree{

    static IfTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.IF, JavaTS.LEFT_ROUND_BRACKET);
        ExpressionTree condition = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);

        StatementTree statement = StatementTree.parse(src);

        if(src.match(JavaTS.ELSE)){
            src.skip(JavaTS.ELSE);
            return new IfTree(condition, statement, StatementTree.parse(src));
        }
        return new IfTree(condition, statement, null);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(3);
        children.add(condition);
        children.add(thenStatement);
        if(elseStatement != null){
            children.add(elseStatement);
        }
        return children;
    }
    
}
