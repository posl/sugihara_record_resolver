package rm4j.compiler.tree;

import java.io.File;
import java.util.Collection;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.core.TokenList;
import rm4j.compiler.core.TokenList.TokenPointer;
import rm4j.compiler.tokens.InputElement;
import rm4j.compiler.tokens.Token;

public class JavaTokenManager{

    public static final boolean ENABLE_PARSING_TRACE = false;

    private final TokenList l;

    public JavaTokenManager(TokenList l){
        this.l = l;
    }

    public final Token lookAhead() throws CompileException{
        return lookAhead(0);
    }
    
    public final Token lookAhead(int pos) throws CompileException{
        var ptr = getPointer();
        for(int i = 0; i < pos && ptr.hasNext(); i++){
            ptr.next();
        }
        return ptr.element();
    }

    public final boolean match(int pos, TokenPointer ptr, JavaTS... symbols) throws CompileException{
        for(int i = 0; i < pos; i++){
            if(!ptr.hasNext()){
                return false;
            }
            ptr.next();
        }
        for(int i = 0, n = symbols.length; i < n && ptr.hasNext(); i++, ptr.next()){
            if(symbols[i] != ptr.element().resolution){
                return false;
            }
        }
        return true;
    }

    public final boolean match(int pos, JavaTS... symbols) throws CompileException{
        return match(pos, getPointer(), symbols);
    }
    
    public final boolean match(TokenPointer ptr, JavaTS... symbols) throws CompileException{
        return match(0, ptr, symbols);
    }

    public final boolean match(JavaTS... symbols) throws CompileException{
        return match(0, symbols);   
    }

    /**
     * Note that{@code pos}starts from 0.
     * @param pos
     * @param symbol
     * @return
     * @throws CompileException
     */

    public final boolean match(int pos, JavaTS symbol) throws CompileException{
        return lookAhead(pos).resolution == symbol;
    }

    /**
     * Note that{@code pos}starts from 0.
     * @param pos
     * @param symbols
     * @return
     * @throws CompileException
     */
    public final boolean match(int pos, Collection<JavaTS> symbols) throws CompileException{
        return symbols.contains(lookAhead(pos).resolution);
    }

    public final boolean match(Collection<JavaTS> symbols) throws CompileException{
        return match(0, symbols);
    }

    public final Token read() throws CompileException{
        while(!(l.refer() instanceof Token) && hasRest()){
            l.get();
        }
        if(!hasRest()){
            throw new ParserException("Read out of bounds.");
        }
        Token t = (Token)l.get();
        if(ENABLE_PARSING_TRACE){
            System.out.print(t);
        }
        return t;
    }

    public final void skip(JavaTS... tokens) throws CompileException{
        for(int i = 0; i < tokens.length; i++){
            InputElement e;
            while(!((e = l.get()) instanceof Token) && hasRest());
            if(!(e instanceof Token)){
                throw new ParserException("Detected Illegal EOF.");
            }
            Token t = (Token)e;
            if(ENABLE_PARSING_TRACE){
                System.out.print(t);
            }
            if(tokens[i] != t.resolution){
                throw new IllegalTokenException(t, tokens[i].key());
            }
        }
    }

    public final boolean hasRest() throws CompileException{
        return lookAhead() != Token.EOF;
    }

    public final void formatGenericsClose(TokenPointer ptr) throws CompileException{
        Token t;
        switch((t = ptr.element()).resolution){
            case BITWISE_SIGNED_RIGHT_SHIFT -> l.split(ptr, new Token(">", t.ref.fix(-1)), new Token(">", t.ref));
            case BITWISE_UNSIGNED_RIGHT_SHIFT -> l.split(ptr, new Token(">", t.ref.fix(-1)), new Token(">>", t.ref));
            case GREATER_THAN ->{}
            default -> throw new IllegalTokenException(lookAhead(), "\">\""); 
        }
    }

    public final void formatGenericsClose() throws CompileException{
        Token t;
        switch((t = lookAhead()).resolution){
            case BITWISE_SIGNED_RIGHT_SHIFT -> l.splitToken(new Token(">", t.ref.fix(-1)), new Token(">", t.ref));
            case BITWISE_UNSIGNED_RIGHT_SHIFT -> l.splitToken(new Token(">", t.ref.fix(-1)), new Token(">>", t.ref));
            case GREATER_THAN ->{}
            default -> throw new IllegalTokenException(lookAhead(), "\">\""); 
        }
    }

    public final TokenPointer getPointer() throws CompileException{
        return l.new TokenPointer();
    }

    public final File source(){
        return l.file;
    }

}