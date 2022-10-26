package rm4j.compiler.tokens;

public class Separator extends Token{

    public Separator(String text, Reference ref){
        super(text, ref);
    }
    
    @Override
    public String info(){
        return "#" + toString() + "# (Separator : " +resolution.name()+ ")" + ref.toString();
    }

}
