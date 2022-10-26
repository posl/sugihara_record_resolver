package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a member reference expression.
 *
 * For example:
 * <pre>
 *   <em>expression</em> :: <em>[ identifier | new ]</em>
 * </pre>
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record MemberReferenceTree(Tree qualifierExpression, ArrayList<TypeTree> typeArguments, IdentifierTree methodName, ReferenceMode mode) implements ExpressionTree, Accessor{

    /**
     * There are two kinds of member references: (i) method references and
     * (ii) constructor references
     */
    public enum ReferenceMode{
        /** enum constant for method references. */
        INVOKE,
        /** enum constant for constructor references. */
        NEW
    }

    static MemberReferenceTree parse(Tree qualifierExpression, JavaTokenManager src) throws CompileException{
        ArrayList<TypeTree> typeArguments = new ArrayList<>();
        src.skip(JavaTS.DOUBLE_COLON);
        if(src.match(JavaTS.LESS_THAN)){
            typeArguments = Tree.resolveTypeArguments(src);
        }
        if(src.match(JavaTS.NEW)){
            src.skip(JavaTS.NEW);
            return new MemberReferenceTree(qualifierExpression, typeArguments, IdentifierTree.EMPTY, ReferenceMode.NEW);
        }
        return new MemberReferenceTree(qualifierExpression, typeArguments, IdentifierTree.parse(src), ReferenceMode.INVOKE);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        children.add(qualifierExpression);
        children.addAll(typeArguments);
        children.add(methodName);
        return children;
    }

}
