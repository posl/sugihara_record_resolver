package rm4j.compiler.tree;

import rm4j.test.Tested;
import rm4j.test.Tested.Status;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;

/**
 * A tree node for a{@code continue}statement.
 *
 * For example:
 * <pre>
 *   continue;
 *   continue <em>label</em> ;
 * </pre>
 *
 * @jls 14.16 The continue Statement
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.CLEARLY_OK)
public record ContinueTree(IdentifierTree label) implements StatementTree, Accessor{

    static ContinueTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.CONTINUE);
        IdentifierTree label = null;
        if(src.match(IDENTIFIERS)){
            label = IdentifierTree.parse(src);
        }
        src.skip(JavaTS.SEMICOLON);
        return new ContinueTree(label);
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
