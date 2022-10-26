package rm4j.compiler.core;

public class IllegalSymbolException extends LexerException{

    public IllegalSymbolException(String message){
        super(message);
    }

    public IllegalSymbolException(JavaTS s){
        this("Illegal symbol : "+s.toString());
    }
}
