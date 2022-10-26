package rm4j.compiler.tree;

import rm4j.compiler.core.ParserException;
import rm4j.compiler.tokens.Token;

public class IllegalTokenException extends ParserException{

    public IllegalTokenException(Token err, String expected){
        super(String.format("Illegal token \"%s\", expected %s.", err.text, expected));
    }
}
