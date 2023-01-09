package rm4j.compiler.resolution;

import rm4j.compiler.core.JavaTS;
import rm4j.compiler.tokens.Token;
import rm4j.compiler.tree.IllegalTokenException;

public enum PrimitiveType{
    BOOLEAN(JavaTS.BOOLEAN),
    BYTE(JavaTS.BYTE),
    SHORT(JavaTS.SHORT),
    CHAR(JavaTS.CHAR),
    INT(JavaTS.INT),
    LONG(JavaTS.LONG),
    FLOAT(JavaTS.FLOAT),
    DOUBLE(JavaTS.DOUBLE);

    private JavaTS symbol;

    private PrimitiveType(JavaTS symbol){
        this.symbol = symbol;
    }

    public JavaTS symbol(){
        return symbol;
    }

    public static PrimitiveType get(Token t) throws IllegalTokenException{
        for(PrimitiveType p : PrimitiveType.values()){
            if(p.symbol == t.resolution){
                return p;
            }
        }
        throw new IllegalTokenException(t, "primitive type");
    }

}
