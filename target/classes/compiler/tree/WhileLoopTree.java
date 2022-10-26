package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code while}loop statement.
 *
 * For example:
 * <pre>
 *   while ( <em>condition</em> )
 *     <em>statement</em>
 * </pre>
 *
 *
 * @jls 14.12 The while Statement
 *
 * @author me
 */


@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record WhileLoopTree(ExpressionTree condition, StatementTree statement) implements StatementTree{

    static WhileLoopTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.WHILE, JavaTS.LEFT_ROUND_BRACKET);
        ExpressionTree condition = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        return new WhileLoopTree(condition, StatementTree.parse(src));
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(condition);
        children.add(statement);
        return children;
    }

}
