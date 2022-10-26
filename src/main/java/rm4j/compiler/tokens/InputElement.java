package rm4j.compiler.tokens;

public abstract class InputElement{
    public final String text;
    public Reference ref;

    public InputElement(String text, Reference ref){
        this.text = text;
        this.ref = ref;
    }

    @Override
    public String toString(){
        return text;
    }

    public String info(){
        return "#" + text + "#" + ref.toString();
    }
}
