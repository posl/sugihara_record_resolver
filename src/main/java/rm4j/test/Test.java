package rm4j.test;

import java.io.File;
import java.io.IOException;

import rm4j.io.DatasetManager;

public class Test{

    public static File debug = new File("./");

    public static void main(String[] args){
        Timer test1 = new Timer(() ->{
            try{
                DatasetManager manager = new DatasetManager();
                manager.getMetrics();
                manager.countTypeDeclarations();
            }catch(IOException e){
                e.printStackTrace();
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
