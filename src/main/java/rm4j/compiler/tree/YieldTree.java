package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code yield}statement.
 *
 * For example:
 * <pre>
 *   yield <em>expression</em> ;
 * </pre>
 *
 * @jls 14.21 The yield Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.MAYBE_OK)
public record YieldTree(ExpressionTree value) implements StatementTree{

    static YieldTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.YIELD);
        var value = ExpressionTree.parse(src);
        src.skip(JavaTS.SEMICOLON);
        return new YieldTree(value);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(value);
        return children;
    }

}
