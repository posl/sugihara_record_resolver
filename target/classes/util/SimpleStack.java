package rm4j.util;

/**
 * This is a normal stack.
 * 
 * @author me
 */
public class SimpleStack<E> extends SimpleLinkedList<E> implements Cloneable{

    @SafeVarargs
    public SimpleStack(E... initialTokens){
        this.size = 0;
        this.top = null;
        for(E token : initialTokens){
            push(token);
        }
    }

    public void push(E token){
        top = new Node(token, top);
        size++;
    }

    public E pop() throws IndexOutOfBoundsException{
        if(isEmpty()){
            throw new IndexOutOfBoundsException("'get()' operation to an empty stack");
        }else{
            E e = top.element;
            top = top.next;
            size--;
            return e;
        }
    }

    @Override
    public void add(E token){
        push(token);
    }

    @Override
    public E get() throws IndexOutOfBoundsException{
        return pop();
    }
    
    @Override
    public SimpleStack<E> clone(){
        Node n = top;
        SimpleStack<E> s = new SimpleStack<>();
        while(n != null){
            s.push(n.element);
            n = n.next;
        }
        return s;
    }

}