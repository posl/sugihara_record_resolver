package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a{@code catch}block in a{@code try}statement.
 *
 * For example:
 * <pre>
 *   catch ( <em>parameter</em> )
 *       <em>block</em>
 * </pre>
 *
 * @jls 14.20 The try statement
 *
 * @author me
 */
public record CatchTree(VariableTree parameter, BlockTree block) implements Tree{
    
    static CatchTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.CATCH, JavaTS.LEFT_ROUND_BRACKET);
        VariableTree parameter = VariableTree.resolveCatchFormalParameter(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        return new CatchTree(parameter, BlockTree.parse(src));
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(parameter);
        children.add(block);
        return children;
    }
    
}
