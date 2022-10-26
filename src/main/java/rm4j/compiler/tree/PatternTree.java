package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A common interface for patterns.
 * @author me
 */

public interface PatternTree extends CaseConstantTree{

    static PatternTree parse(JavaTokenManager src) throws CompileException{
        if(src.match(JavaTS.LEFT_ROUND_BRACKET)){
            return ParenthesizedPatternTree.parse(src);
        }
        if(Tree.lookAhead(src, LookAheadMode.TYPE) == JavaTS.LEFT_ROUND_BRACKET){
            return RecordPatternTree.parse(src);
        }
        return TypePatternTree.parse(src);
    }

    static boolean followsPattern(JavaTokenManager src) throws CompileException{
        var ptr = src.getPointer();
        while(ptr.match(JavaTS.LEFT_ROUND_BRACKET)){
            ptr.next();
        }
        LookAheadMode.MODIFIERS.skip(ptr);
        if(!LookAheadMode.TYPE.skip(ptr)){
            return false;
        }
        if(ptr.match(JavaTS.LEFT_ROUND_BRACKET)){
            if(!LookAheadMode.INSIDE_BRACKETS.skip(ptr)){
                return false;
            }
        }else{
            if(!ptr.match(IDENTIFIERS)){
                return false;
            }
        }
        return true;
    }

}
