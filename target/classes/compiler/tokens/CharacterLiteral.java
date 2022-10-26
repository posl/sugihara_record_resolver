package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;

public class CharacterLiteral extends Literal{

    public CharacterLiteral(String text, Reference ref){
        super(text, ref, JavaTS.CHARACTER_LITERAL);
    }

    @Override
    public String toString(){
        return "\'" + text + "\'";
    }
    
}
