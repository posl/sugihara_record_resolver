package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a statement block.
 *
 * For example:
 * <pre>
 *  { }
 *
 *  { <em>statements</em> }
 *
 *   static{ <em>statements</em> }
 * </pre>
 *
 * @jls 14.2 Blocks
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record BlockTree(boolean isStatic, ArrayList<StatementTree> statements)implements StatementTree{

    static BlockTree parse(JavaTokenManager src) throws CompileException{
        boolean isStatic;
        ArrayList<StatementTree> statements = new ArrayList<>();
        if(isStatic = src.match(JavaTS.STATIC)){
            src.skip(JavaTS.STATIC);
        }
        src.skip(JavaTS.LEFT_CURLY_BRACKET);
        while(!src.match(JavaTS.RIGHT_CURLY_BRACKET)){
            statements.add(StatementTree.resolveBlockStatement(src));
        }
        src.skip(JavaTS.RIGHT_CURLY_BRACKET);
        return new BlockTree(isStatic, statements);
    }   

    @Override
    public List<Tree> children(){
        return new ArrayList<>(statements);
    }
    
}
