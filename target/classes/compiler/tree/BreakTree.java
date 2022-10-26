package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a{@code break}statement.
 *
 * For example:
 * <pre>
 *   break;
 *
 *   break <em>label</em> ;
 * </pre>
 *
 * @jls 14.15 The break Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.CLEARLY_OK) 
public record BreakTree(IdentifierTree label) implements StatementTree, Accessor{

    static BreakTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.BREAK);
        IdentifierTree label = null;
        if(src.match(IDENTIFIERS)){
            label = IdentifierTree.parse(src);
        }
        src.skip(JavaTS.SEMICOLON);
        return new BreakTree(label);
    }
    
    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        if(label != null){
            children.add(label);
        }
        return children;
    }
    
}
