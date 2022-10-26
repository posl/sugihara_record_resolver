package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for an 'opens' directive in a module declaration.
 *
 * For example:
 * <pre>
 *    opens   <em>package-name</em>;
 *    opens   <em>package-name</em> to <em>module-name</em>;
 * </pre>
 *
 * @author me
 */
public record OpensTree(ExpressionNameTree packageName, ArrayList<ExpressionNameTree> moduleNames) implements DirectiveTree{

    static OpensTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.OPENS);
        var packageName = ExpressionNameTree.parse(src);

        ArrayList<ExpressionNameTree> moduleNames = new ArrayList<>();
        if(src.match(JavaTS.TO)){
            do{
                src.read();
                moduleNames.add(ExpressionNameTree.parse(src));
            }while(src.match(JavaTS.COMMA));
        }
        src.skip(JavaTS.SEMICOLON);
        return new OpensTree(packageName, moduleNames);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        children.add(packageName);
        children.addAll(moduleNames);
        return children;
    }
    
}
