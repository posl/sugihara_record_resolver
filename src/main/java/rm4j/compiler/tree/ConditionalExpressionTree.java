package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for the conditional operator{@code ? :}.
 *
 * For example:
 * <pre>
 *   <em>condition</em> ? <em>trueExpression</em> : <em>falseExpression</em>
 * </pre>
 *
 * @jls 15.25 Conditional Operator ? :
 *
 * @author me
 */ 

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ConditionalExpressionTree(ExpressionTree condition, ExpressionTree trueExpression, ExpressionTree falseExpression) implements ExpressionTree{

    static ConditionalExpressionTree parse(ExpressionTree condition, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.QUESTION);
        ExpressionTree trueExpression = ExpressionTree.parse(src);
        src.skip(JavaTS.COLON);
        return new ConditionalExpressionTree(condition, trueExpression,
            ExpressionTree.followsLambdaExpression(src)? LambdaExpressionTree.parse(src) : ExpressionTree.resolveConditionalExpression(src));
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(3);
        children.add(condition);
        children.add(trueExpression);
        children.add(falseExpression);
        return children;
    }
    
}
