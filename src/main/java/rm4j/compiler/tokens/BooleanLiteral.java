package rm4j.compiler.tokens;

import rm4j.compiler.resolution.PrimitiveType;

public class BooleanLiteral extends Literal{

    public BooleanLiteral(boolean tf, Reference ref){
        super(String.valueOf(tf), ref);
        this.type = PrimitiveType.BOOLEAN;
    }
    
}
