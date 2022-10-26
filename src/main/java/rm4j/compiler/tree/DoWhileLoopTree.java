package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code do}statement.
 *
 * For example:
 * <pre>
 *   do
 *       <em>statement</em>
 *   while ( <em>expression</em> );
 * </pre>
 *
 * @jls 14.13 The do Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record DoWhileLoopTree(ExpressionTree condition, StatementTree statement) implements StatementTree{

    static DoWhileLoopTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.DO);
        var statement = StatementTree.parse(src);
        src.skip(JavaTS.WHILE, JavaTS.LEFT_ROUND_BRACKET);
        var condition = ExpressionTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET, JavaTS.SEMICOLON);
        return new DoWhileLoopTree(condition, statement);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(condition);
        children.add(statement);
        return children;
    }

}