package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a parenthesized expression.  Note: parentheses
 * not be preserved by the parser.
 *
 * For example:
 * <pre>
 *   ( <em>expression</em> )
 * </pre>
 *
 * @jls 15.8.5 Parenthesized Expressions
 *
 * @author me
 * 
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ParenthesizedTree(ExpressionTree expression)implements ExpressionTree{

    static ParenthesizedTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.LEFT_ROUND_BRACKET);
        ExpressionTree expression = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        return new ParenthesizedTree(expression);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(expression);
        return children;
    }

}
