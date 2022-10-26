package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for an array access expression.
 *
 * For example:
 * <pre>
 *   <em>expression</em> [ <em>index</em> ]
 * </pre>
 *
 * @jls 15.10.3 Array Access Expressions
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ArrayAccessTree(ExpressionTree expression, ExpressionTree index)implements ExpressionTree{

    static ArrayAccessTree parse(ExpressionTree expression, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.LEFT_SQUARE_BRACKET);
        ExpressionTree index = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_SQUARE_BRACKET);
        return new ArrayAccessTree(expression, index);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(expression);
        children.add(index);
        return children;
    }

}
