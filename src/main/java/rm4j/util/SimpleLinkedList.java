package rm4j.util;

import rm4j.compiler.core.CompileException;
import rm4j.util.functions.CEFunction;

public abstract class SimpleLinkedList<E>{

    protected int size;
    protected Node top;

    public abstract E get();
    public abstract void add(E e);

    public int size(){
        return size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public E getTopToken(){
        if(size == 0){
            return null;
        }else{
            return top.element;
        }
    }

    public void clear(){
        while(!isEmpty()){
            get();
        }
    }

    public String toString(CEFunction<E, String> rule) throws CompileException{
        Node current = top;
        String s = "[";
        while(current != null){
            s += rule.apply(current.element);
            current = current.next;
            if(current == null){
                break;
            }
            s += ", ";
        }
        s += "]";
        return s;
    }

    @Override
    public String toString(){
        try{
            return toString(e -> e.toString());
        }catch(CompileException e){
            return "#CompileException";
        }
    }

    protected class Node{
        public E element;
        public Node next;

        public Node(E element, Node next){
            this.element = element;
            this.next = next;
        }
    }

}