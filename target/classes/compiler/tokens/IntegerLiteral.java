package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;

public class IntegerLiteral extends Literal{

    final int radix;

    public IntegerLiteral(String text, Reference ref, int radix){
        super(text, ref, JavaTS.INTEGER_LITERAL);
        this.radix = radix;
    }
    
    @Override
    public String info(){
        return "#" + toString() + "# (" +resolution.name()+"("+ radix +"))" + ref.toString();
    }

}
