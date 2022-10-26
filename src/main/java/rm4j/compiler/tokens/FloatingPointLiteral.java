package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;

public class FloatingPointLiteral extends Literal{
    final int radix;

    public FloatingPointLiteral(String text, Reference ref, int radix){
        super(text, ref, JavaTS.FLOATING_POINT_LITERAL);
        this.radix = radix;
    }
    
    @Override
    public String info(){
        return "#" + toString() + "# (" +resolution.name()+"("+ radix +"))" + ref.toString();
    }

}
