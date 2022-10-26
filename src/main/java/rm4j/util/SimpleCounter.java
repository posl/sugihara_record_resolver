package rm4j.util;

public class SimpleCounter{
    
    private final boolean allowMinus;

    private volatile int count = 0;

    public SimpleCounter(){
        allowMinus = true;
    }

    public SimpleCounter(boolean allowMinus){
        this.allowMinus = allowMinus;
    }


    public void countUp(){
        count++;
    }

    public void countDown() throws IndexOutOfBoundsException{
        if(count == 0 && !allowMinus){
            throw new IndexOutOfBoundsException("Count of below 0.");
        }
        count--;
    }

    public int getCount(){
        return count;
    }

}
