package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a class literal expression.
 * For example:
 * <pre>
 *   <em>String</em>.<em>clasa</em>
 * </pre>
 *
 * @jls 15.8.2 Class Literals
 *
 * @author me;
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ClassLiteralTree(Tree type) implements ExpressionTree{

    static ClassLiteralTree parse(Tree type, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.PERIOD, JavaTS.CLASS);
        return new ClassLiteralTree(type);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(type);
        return children;
    }

}
