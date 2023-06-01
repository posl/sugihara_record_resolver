package rm4j.compiler.core;

import java.io.File;
import java.util.Set;

import rm4j.compiler.tokens.InputElement;
import rm4j.compiler.tokens.Reference;
import rm4j.compiler.tokens.Token;
import rm4j.compiler.tree.FatalParserError;
import rm4j.util.DoublyLinkedList;
import rm4j.util.functions.CEPredicate;

/**
 * This is a singly-linked list with a pointer.
 * 
 * @author me
 */
public class TokenList extends DoublyLinkedList<InputElement>{

    public File file;

    /**
     * Creates CharList of the specified characters.
     */
    public TokenList(File file, InputElement... initialTokens){
        this.file = file;
        for(InputElement e : initialTokens){
            add(e);
        }
    }

    @SafeVarargs
    public final InputElement[] referWithCondition(CEPredicate<? super InputElement>... conds) throws CompileException{
        var elements = new InputElement[conds.length];
        Node curr = current;
        for(int i = 0; i < conds.length; i++){
            while(!conds[i].test(curr.e)){
                if(curr == bottom){
                    var ret = new InputElement[i];
                    for(int j = 0; j < i; j++){
                        ret[j] = elements[j];
                    }
                    return ret;
                }
                curr = curr.next;
            }
            elements[i] = curr.e;
        }
        return elements;
    }

    public final InputElement[] lookAhead(int length, CEPredicate<? super InputElement> cond) throws CompileException{
        var elements = new InputElement[length];
        Node curr = current;
        for(int i = 0; i < length; i++){
            while(!cond.test(curr.e)){
                if(curr == bottom){
                    var ret = new InputElement[i];
                    for(int j = 0; j < i; j++){
                        ret[j] = elements[j];
                    }
                    return ret;
                }
                curr = curr.next;
            }
            elements[i] = curr.e;
            curr = curr.next;
        }
        return elements;
    }

    /**
     * Reads back the characters which satisfies the specified condition from the current pointer.
     */
    @SafeVarargs
    public final boolean applyTests(CEPredicate<? super InputElement>... testers) throws CompileException{
        Node curr = current;
        for(CEPredicate<? super InputElement> cond : testers){
            if(curr == top || !cond.test(curr.e)){
                return false;
            }
            curr = curr.prev;
        }
        return true;
    }

    public final void split(TokenPointer ptr, InputElement e1, InputElement e2) throws CompileException{
        if(ptr.pointer == top || ptr.pointer.prev == top){
            throw new FatalParserError("Illegal token operation : Cannot apply 'split'.");
        }
        Node curr =  ptr.pointer;
        curr = curr.prev;
        curr.next = new Node(e1, curr, null);
        curr = curr.next;
        curr.next = new Node(e2, curr, ptr.pointer.next);
        ptr.pointer.next.prev = curr.next;
        ptr.pointer = curr;
    }

    public final void splitToken(Token t1, Token t2) throws CompileException{
        if(current == top || current.prev == top){
            throw new FatalParserError("Illegal token operation : Cannot apply 'split'.");
        }
        while(!(current.e instanceof Token)){
            current = current.next;
        }
        Node curr =  current;
        curr = curr.prev;
        curr.next = new Node(t1, curr, null);
        curr = curr.next;
        curr.next = new Node(t2, curr, current.next);
        current.next.prev = curr.next;
        current = curr;
    }

    public final Reference getCurrentReference(){
        if(current.e == null){
            return new Reference(1, 0);
        }else{
            return current.e.ref;
        }
    }

    public class NodeAccessor{
        private Node n;

        public NodeAccessor(){
            this.n = current;
        }
        
        public InputElement curr(){
            return n.e;
        }

        public void next(){
            n = n.next;
        }
    }

    public final class TokenPointer implements Cloneable{
        private Node pointer;

        public TokenPointer() throws CompileException{
            Node curr = current;
            while(!(curr.e instanceof Token)){
                curr = curr.next;
            }
            this.pointer = curr;
        }

        private TokenPointer(Node pointer){
            this.pointer = pointer;
        }

        public Token element() throws CompileException{
            return (Token)pointer.e;
        }

        public void next() throws CompileException{
            do{
                pointer = pointer.next;
            }while(!(pointer.e instanceof Token));
        }

        public boolean hasNext(){
            return pointer != bottom;
        }

        public boolean match(JavaTS... symbols) throws CompileException{
            Node curr = pointer;
            for(int i = 0, n = symbols.length; i < n && curr != bottom; i++){
                if(symbols[i] != ((Token)curr.e).resolution){
                    return false;
                }
                do{
                    curr = curr.next;
                }while(!(curr.e instanceof Token));
            }
            return true;
        }

        public boolean match(Set<JavaTS> symbols) throws CompileException{
            return symbols.contains(element().resolution);
        }

        public boolean match(int pos, JavaTS symbol) throws CompileException{
            Node curr = pointer;
            for(int i = 0; i < pos && curr != bottom; i++){
                do{
                    curr = curr.next;
                }while(!(curr.e instanceof Token));
            }
            return ((Token)curr.e).resolution == symbol;
        }

        public boolean match(int pos, Set<JavaTS> symbols) throws CompileException{
            Node curr = pointer;
            for(int i = 0; i < pos && curr != bottom; i++){
                do{
                    curr = curr.next;
                }while(!(curr.e instanceof Token));
            }
            return symbols.contains(((Token)curr.e).resolution);
        }

        public void recover(TokenPointer ptr){
            this.pointer = ptr.pointer;
        }

        public void setProvisionalToken(JavaTS symbol){
            Token t = new Token(symbol.name(), null);
            pointer = new Node(t, null, pointer);
        }

        @Override
        public TokenPointer clone(){
            return new TokenPointer(pointer);
        }

    }

}
