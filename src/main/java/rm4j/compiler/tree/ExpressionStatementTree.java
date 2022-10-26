package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.tree.UnaryTree.UnaryOperator;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for an expression statement.
 *
 * For example:
 * <pre>
 *   <em>expression</em> ;
 * </pre>
 *
 * @jls 14.8 Expression Statements
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ExpressionStatementTree(ExpressionTree expression) implements StatementTree{

    static ExpressionStatementTree parse(JavaTokenManager src) throws CompileException{
        ExpressionStatementTree statementExpression = resolveStatementExpression(src);
        src.skip(JavaTS.SEMICOLON);
        return statementExpression;
    }

    static ExpressionStatementTree resolveStatementExpression(JavaTokenManager src) throws CompileException{
        ExpressionTree expr = ExpressionTree.parse(src);
        if(expr instanceof AssignmentTree
            || expr instanceof CompoundAssignmentTree
            || expr instanceof MethodInvocationTree
            || expr instanceof NewClassTree
            || (expr instanceof UnaryTree u
                && (u.operatorType() == UnaryOperator.PREFIX || u.operatorType() == UnaryOperator.POSTFIX)
                && (u.operatorToken() == JavaTS.INCREMENT || u.operatorToken() == JavaTS.DECREMENT))){
            return new ExpressionStatementTree(expr);
        }else{
            throw new ParserException("Illegal statement expression, expected increment/decrement expression, method invocation, class instance creation expression and assignment.");
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(expression);
        return children;
    }

}
