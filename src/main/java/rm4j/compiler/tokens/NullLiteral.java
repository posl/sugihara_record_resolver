package rm4j.compiler.tokens;

import rm4j.compiler.tree.TypeTree;

public class NullLiteral extends Literal{

    public NullLiteral(Reference ref){
        super("null", ref);
        this.type = TypeTree.NULL;
    }
    
}
