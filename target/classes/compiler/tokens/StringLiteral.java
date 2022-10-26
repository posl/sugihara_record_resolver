package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;
import rm4j.compiler.tree.TypeTree;

public class StringLiteral extends Literal{

    public StringLiteral(String text, Reference ref){
        super(text, ref, JavaTS.STRING_LITERAL);
        this.type = TypeTree.STRING;
    }

    @Override
    public String toString(){
        return "\"" + text + "\"";
    }
    
}
