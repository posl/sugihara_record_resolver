package rm4j.compiler.tokens;

import rm4j.compiler.core.JavaTS;

public class Token extends InputElement{
    public static final Token EOF = new Token("#eof", Reference.NULL);

    public final JavaTS resolution;

    public Token(String text, Reference ref){
        super(text, ref);
        this.resolution = JavaTS.set(text);
    }

    public Token(String text, Reference ref, JavaTS resolution){
        super(text, ref);
        this.resolution = resolution;
    }
    
    @Override
    public String info(){
        return "#" + toString() + "# (" +resolution.name()+ ")" + ref.toString();
    }
}
