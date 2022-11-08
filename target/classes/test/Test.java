package rm4j.test;

import java.io.File;
import java.io.IOException;

import rm4j.io.git.DatasetManager;

public class Test{

    public static File debug = new File("./");

    public static void main(String[] args){
        Timer test1 = new Timer(() ->{
            try{
                DatasetManager manager = new DatasetManager();
                manager.collectDataOfSingleTrace(f -> true);
            }catch(IOException e){
                System.out.println(e);
            }
        });
        test1.run();
    }

    public static File debugFile(){
        return new File("./org/posl/test/TestSource.java");
    }

    @FunctionalInterface
    public interface IOConsumer<T>{
        public void accept(T t) throws IOException;
    }

}
