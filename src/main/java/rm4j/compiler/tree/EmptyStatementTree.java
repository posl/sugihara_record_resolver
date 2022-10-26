package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for an empty (skip) statement.
 *
 * For example:
 * <pre>
 *    ;
 * </pre>
 *
 * @jls 14.6 The Empty Statement
 *
 * @author me
 */

public record EmptyStatementTree() implements StatementTree{

    static EmptyStatementTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.SEMICOLON);
        return new EmptyStatementTree();
    }

}
