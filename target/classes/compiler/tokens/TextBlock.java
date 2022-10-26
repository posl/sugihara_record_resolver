package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;
import rm4j.compiler.tree.TypeTree;

public class TextBlock extends Literal{
    public final String space;

    public TextBlock(String space, String text, Reference ref){
        super(text, ref, JavaTS.TEXT_BLOCK);
        this.space = space;
        this.type = TypeTree.STRING;
    }

    @Override
    public String toString(){
        return "\"\"\"" + space + text + "\"\"\"";
    }
    
}
