package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.tokens.Literal;
import rm4j.compiler.tokens.Token;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a literal expression.
 * Use{@link #getKind getKind}to determine the kind of literal.
 *
 * For example:
 * <pre>
 *   <em>value</em>
 * </pre>
 *
 * @jls 15.28 Constant Expressions
 *
 * @author me
 * 
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record LiteralTree(String value) implements ExpressionTree{
    
    static LiteralTree parse(JavaTokenManager src) throws CompileException{
        Token t = src.read();
        if(t instanceof Literal l){
            return new LiteralTree(l.text);
        }
        throw new IllegalTokenException(t, "literal");
    }

}
