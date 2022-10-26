package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;
import rm4j.compiler.tree.TypeTree;

public abstract class Literal extends Token{
    TypeTree type;

    public Literal(String text, Reference ref){
        super(text, ref);
    }

    public Literal(String text, Reference ref, JavaTS resolution){
        super(text, ref, resolution);
    }

}
