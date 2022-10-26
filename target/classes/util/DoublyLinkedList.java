package rm4j.util;

public class DoublyLinkedList<E>{
    protected final Node top = new Node(null, null, null);
    protected final Node bottom = new Node(null, null, null);

    protected Node current = top;

   {
        top.next = bottom;
        bottom.prev = top;
    }
    
    /**
     * Creates linked list of the specified elements.
     */
    @SafeVarargs
    public DoublyLinkedList(E... initials){
        for(E e : initials){
            add(e);
        }
    }

    /**
     * Adds a element to the end of a list.
     * The pointer is set to the added element.
     */
    public void add(E e){
        Node n = new Node(e, bottom.prev, bottom);
        bottom.prev.next = n;
        bottom.prev = n;
        current = n;
    }

    /**
     * Gets a single element.
     * The pointer moves to the next.
     */
    public E get() throws IndexOutOfBoundsException{
        if(hasNext()){
            E e = current.e;
            current = current.next;
            return e;
        }else{
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Looks current element.
     */

    public E refer(){
        return current.e;
    }

    /**
     * Removes the specified number of elements from the end of a list.
     */
    public boolean remove(int size){
        Node curr = current;
        for(int i = 0; i < size; i++){
            if(curr == top){
                return false;
            }
            curr = curr.prev;
        }
        curr.next = current.next;
        current.next.prev = curr;
        current = curr;
        return true;
    }

    /**
     * Sets the pointer to the first element of a list.
     */
    public void reset(){
        current = top.next;
    }

    /**
     * Checks whether a list has at least one charactrer.
     */
    public boolean isEmpty(){
        return top.next == bottom;
    }

    /**
     * Checks whether a pointed element has next one.
     */
    public boolean hasNext(){
        return !isEmpty() && current != bottom;
    }

    @Override
    public String toString(){
        String s = "[";
        Node curr = top;
        do{
            curr = curr.next;
            s += curr.e + ", ";
        }while(curr != bottom);
        return s + "]";
    }

    /**
     * Nodes which are used in a linked list. 
     */

    public class Node{
        public final E e;
        public Node prev;
        public Node next;

        public Node(E e, Node prev, Node next){
            this.e = e;
            this.prev = prev;
            this.next = next;
        }
    }
}

