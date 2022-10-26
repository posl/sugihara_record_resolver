package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A case label that marks{@code default}.
 * 
 * @author me
 */
public record DefaultCaseLabelTree() implements SwitchLabelTree, CaseConstantTree{

    static DefaultCaseLabelTree parse(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.DEFAULT);
        return new DefaultCaseLabelTree();
    }

}


