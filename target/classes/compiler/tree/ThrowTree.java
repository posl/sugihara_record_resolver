package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code throw}statement.
 *
 * For example:
 * <pre>
 *   throw <em>expression</em>;
 * </pre>
 *
 * @jls 14.18 The throw Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ThrowTree(ExpressionTree expression) implements StatementTree{

    static ThrowTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.THROW);
        ExpressionTree expression = ExpressionTree.parse(src);
        src.skip(JavaTS.SEMICOLON);
        return new ThrowTree(expression);
    }
    
    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(expression);
        return children;
    }

}
