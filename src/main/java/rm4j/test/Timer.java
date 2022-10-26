package rm4j.test;

public record Timer(Runnable runnable){

    public void run(){
        long time = System.currentTimeMillis();
        runnable.run();
        time = System.currentTimeMillis() - time;
        System.out.println(String.format("%d mili seconds", time));
    }

}
