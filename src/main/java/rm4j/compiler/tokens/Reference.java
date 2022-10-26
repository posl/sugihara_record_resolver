package rm4j.compiler.tokens;

public class Reference{
    public static final Reference NULL = new Reference(0, 0);

    public int line;
    public int pos;

    public Reference(int line, int pos){
        this.line = line;
        this.pos = pos;
    }

    public Reference fix(int i){
        pos += i;
        return this;
    }

    @Override
    public String toString(){
        return "[" + line + ", " + pos + "]";
    }

}
