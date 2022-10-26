package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a 'uses' directive in a module declaration.
 *
 * For example:
 * <pre>
 *    uses <em>service-name</em>;
 * </pre>
 *
 * @author me
 */
public record UsesTree(ExpressionNameTree serviceName)implements DirectiveTree{

    static UsesTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.USES);
        var serviceName = ExpressionNameTree.parse(src);
        src.skip(JavaTS.SEMICOLON);
        return new UsesTree(serviceName);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(serviceName);
        return children;
    }
    
}
