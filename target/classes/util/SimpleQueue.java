package rm4j.util;

/**
 * This is a normal queue.
 * 
 * @author me
 */

public class SimpleQueue<E> extends SimpleLinkedList<E>{

    protected Node bottom;

    @SafeVarargs
    public SimpleQueue(E... initialTokens){
        this.size = 0;
        this.top = null;
        this.bottom = null;
        for(E token : initialTokens){
            add(token);
        }
    }

    public void enqueue(E token){
        Node added = new Node(token, null);
        if(isEmpty()){
            top = added;
        }else{
            bottom.next = added;
        }
        bottom = added;
        size++;
    }

    public E dequeue() throws IndexOutOfBoundsException{
        if(isEmpty()){
            throw new IndexOutOfBoundsException("'get()' operation to an empty queue");
        }else{
            E e = top.element;
            top = top.next;
            size--;
            if(size == 0){
                bottom = null;
            }
            return e;
        }
    }

    @Override
    public void add(E token){
        enqueue(token);
    }

    @Override
    public E get() throws IndexOutOfBoundsException{
        return dequeue();
    }
    
}
