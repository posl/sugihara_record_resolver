package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for an{@code assert}statement.
 *
 * For example:
 * <pre>
 *   assert <em>condition</em> ;
 *
 *   assert <em>condition</em> : <em>detail</em> ;
 * </pre>
 *
 * @jls 14.10 The assert Statement
 *
 * @author me
 * 
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record AssertTree(ExpressionTree condition, ExpressionTree detail) implements StatementTree{

    static AssertTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.ASSERT);
        ExpressionTree condition = ExpressionTree.parse(src);
        ExpressionTree detail = null;
        if(src.match(JavaTS.COLON)){
            src.skip(JavaTS.COLON);
            detail = ExpressionTree.parse(src);   
        }
        src.skip(JavaTS.SEMICOLON);
        return new AssertTree(condition, detail);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(condition);
        if(detail != null){
            children.add(detail);
        }
        return children;
    }

}
