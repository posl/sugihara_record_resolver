package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a parenthesized pattern.
 *
 * For example:
 * <pre>
 *   ( <em>pattern</em> )
 * </pre>
 *
 * @jls 14.30.1 Kinds of Patterns
 *
 * @author me
 */

public record ParenthesizedPatternTree(PatternTree pattern) implements PatternTree{

    static ParenthesizedPatternTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.LEFT_ROUND_BRACKET);
        var pattern = PatternTree.parse(src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        return new ParenthesizedPatternTree(pattern);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(pattern);
        return children;
    }

}
