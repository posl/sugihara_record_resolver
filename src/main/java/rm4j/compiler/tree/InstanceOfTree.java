package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for an{@code instanceof}expression.
 *
 * For example:
 * <pre>
 *   <em>expression</em> instanceof <em>type</em>
 * </pre>
 *
 * @jls 15.20.2 Type Comparison Operator instanceof
 *
 * @author me
 */

public record InstanceOfTree(ExpressionTree expression, Tree type, PatternTree pattern) implements ExpressionTree{

    static InstanceOfTree parse(ExpressionTree expression, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.INSTANCEOF);
        if(PatternTree.followsPattern(src)){
            return new InstanceOfTree(expression, null, PatternTree.parse(src));
        }
        return new InstanceOfTree(expression, NameTree.resolveTypeOrName(src), null);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(3);
        children.add(expression);
        if(type != null){
            children.add(type);
        }
        if(pattern != null){
            children.add(pattern);
        }
        return children;
    }

}

