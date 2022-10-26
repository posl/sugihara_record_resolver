package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code return}statement.
 *
 * For example:
 * <pre>
 *   return;
 *   return <em>expression</em>;
 * </pre>
 *
 * @jls 14.17 The return Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ReturnTree(ExpressionTree expression) implements StatementTree{
    
    static ReturnTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.RETURN);
        ExpressionTree expression = null;
        if(!src.match(JavaTS.SEMICOLON)){
            expression = ExpressionTree.parse(src);
        }
        src.skip(JavaTS.SEMICOLON);
        return new ReturnTree(expression);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        if(expression != null){
            children.add(expression);
        }
        return children;
    }
    
}
