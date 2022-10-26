package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * Common tree node for case constants.
 * Patterns, constant expressions,{@code default}are allowed as case constants.
 * 
 * @author me 
 */

public interface CaseConstantTree extends Tree{
    
    static CaseConstantTree parse(JavaTokenManager src) throws CompileException{
        if(src.match(JavaTS.DEFAULT)){
            return DefaultCaseLabelTree.parse(src);
        }
        return ExpressionTree.resolveConditionalExpression(src);
    }

}
