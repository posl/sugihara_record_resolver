package rm4j.compiler.core;

import java.util.Set;

import rm4j.util.DoublyLinkedList;
import rm4j.util.functions.CEPredicate;

/**
 * This is a doubly-linked list of characters with an private pointer.
 * It supports some operations which are used in the lexer.
 * @author me
 */

class CharList extends DoublyLinkedList<Character>{
    private int line = 1;
    
    /**
     * Creates CharList of the specified characters.
     */
    public CharList(char... initialChars){
        for(char c : initialChars){
            add(c);
        }
    }

    /**
     * Reads back the specified number of characters from the current pointer.
     */
    public final String readBack(int size){
        String s = "";
        Node curr = current;
        for(int i = 0; i < size; i++){
            if(curr == top) return s;
            s = curr.e + s;
            curr = curr.prev;
        }
        return s;
    }

    /**
     * Reads back the characters which satisfies the specified condition from the current pointer.
     */
    public final String readBack(CEPredicate<Character> cond) throws CompileException{
        String s = "";
        Node curr = current;
        while(cond.test(curr.e)){
            if(curr == top) return s;
            s = curr.e + s;
            curr = curr.prev;
        }
        return s;
    }

    /**
     * Reads back the characters which are contained in the specified sets from the current pointer.
     */
    @SafeVarargs
    public final String readBack(Set<Character>... charSets) throws CompileException{
        return readBack(c ->
       {
            for(Set<Character> set : charSets){
                if(set.contains(c)) return true;
            }
            return false;
        });
    }

    /**
     * Reads back the characters which are contained in the specified sets from the current pointer.
     */
    public final String readForward(int size){
        String s = "";
        for(int i = 0; i < size && hasNext(); i++){
            s += get();
        }
        return s;
    }

    /**
     * Reads the characters which satisfies the specified condition from the current pointer.
     */
    public final String readForward(CEPredicate<Character> cond) throws CompileException{
        String s = "";
        while(hasNext() && cond.test(current.e)){
            s += get();
        }
        return s;
    }

    /**
     * Reads the characters which are contained in the specified sets from the current pointer.
     */
    @SafeVarargs
    public final String readForward(Set<Character>... charSets) throws CompileException{
        return readForward(c ->
       {
            for(Set<Character> set : charSets){
                if(set.contains(c)) return true;
            }
            return false;
        });
    }

    public int line(){
        return line;
    }

    @Override
    public void reset(){
        current = top.next;
        line = 1;
    }

    @Override
    public final Character get() throws IndexOutOfBoundsException{
        if(hasNext()){
            Character c = current.e;
            current = current.next;
            if(c == '\n'){
                line++;
            }else if(c == '\r' && current.e != null && current.e != '\n'){
                line++;
            }
            return c;
        }else{
            throw new IndexOutOfBoundsException();
        }
    }

}