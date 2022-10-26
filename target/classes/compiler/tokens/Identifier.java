package rm4j.compiler.tokens;

public class Identifier extends Token{

    public Identifier(String text, Reference ref){
        super(text, ref);
    }

    @Override
    public String info(){
        return "#" + toString() + "# (Identifier : " +resolution.name()+ ")" + ref.toString();
    }
    
}
