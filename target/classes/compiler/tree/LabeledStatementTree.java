package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessible;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a labeled statement.
 *
 * For example:
 * <pre>
 *   <em>label</em> : <em>statement</em>
 * </pre>
 *
 * @jls 14.7 Labeled Statements
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record LabeledStatementTree(IdentifierTree label, StatementTree statement) implements StatementTree, Accessible{

    static LabeledStatementTree parse(JavaTokenManager src) throws CompileException{
        var label = IdentifierTree.parse(src);
        src.skip(JavaTS.COLON);
        return new LabeledStatementTree(label, StatementTree.parse(src));
    }

    @Override
    public IdentifierTree simpleName(){
        return label;
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(label);
        children.add(statement);
        return children;
    }
    
}
