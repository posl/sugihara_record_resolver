package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a 'provides' directive in a module declaration.
 *
 * For example:
 * <pre>
 *    provides <em>service-name</em> with <em>implementation-name</em>;
 * </pre>

 * @author me
 */
public record ProvidesTree(ExpressionNameTree serviceName, ArrayList<ExpressionNameTree> implementionNames)implements DirectiveTree{

    static ProvidesTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.PROVIDES);
        var serviceName = ExpressionNameTree.parse(src);

        ArrayList<ExpressionNameTree> implementionNames = new ArrayList<>();
        src.skip(JavaTS.WITH);
        implementionNames.add(ExpressionNameTree.parse(src));
        while(src.match(JavaTS.COMMA)){
            src.skip(JavaTS.COMMA);
            implementionNames.add(ExpressionNameTree.parse(src));
        }
        src.skip(JavaTS.SEMICOLON);
        return new ProvidesTree(serviceName, implementionNames);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        children.add(serviceName);
        children.addAll(implementionNames);
        return children;
    }

}
