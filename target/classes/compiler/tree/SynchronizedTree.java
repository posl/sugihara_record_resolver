package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code synchronized}statement.
 *
 * For example:
 * <pre>
 *   synchronized ( <em>expression</em> )
 *       <em>block</em>
 * </pre>
 *
 * @jls 14.19 The synchronized Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record SynchronizedTree(ExpressionTree expression, BlockTree block) implements StatementTree{

    static SynchronizedTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.SYNCHRONIZED, JavaTS.LEFT_ROUND_BRACKET);
        var expression = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        var block = BlockTree.parse(src);
        return new SynchronizedTree(expression, block);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(expression);
        children.add(block);
        return children;
    }

}
