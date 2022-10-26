package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.resolution.Accessor;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * <p>
 * A tree node for a member access expression.
 * Note that preceding expression cannot be a conjuction of identifiers.
 * A conjuction of identifiers is classified as{@code ExpressionName}.
 * </p>
 * For example:
 * <pre>
 *   <em>expression</em> . <em>identifier</em>
 * </pre>
 *
 * @jls 6.5 Determining the Meaning of a Name
 * @jls 15.11 Field Access Expressions
 * @jls 15.12 Method Invocation Expressions
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record MemberSelectTree(ExpressionTree expression, IdentifierTree identifier) implements ExpressionTree, Accessor{

    static MemberSelectTree parse(ExpressionTree expression, JavaTokenManager src) throws CompileException{
        return new MemberSelectTree(expression, IdentifierTree.parse(src));
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(expression);
        children.add(identifier);
        return children;
    }

}