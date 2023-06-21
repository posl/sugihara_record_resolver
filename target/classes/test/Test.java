package rm4j.test;

import java.io.File;
import java.io.IOException;

import rm4j.io.DatasetManager;
import rm4j.io.TypeComparator;
import rm4j.io.Metrics.JavaVersion;

public class Test{


    public static File debug = new File("./");

    public static void main(String[] args){

        Timer test1 = new Timer(() -> {
            try{
                DatasetManager manager = new DatasetManager();
                manager.getMetrics();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test2 = new Timer(() -> {
            try{
                DatasetManager manager = new DatasetManager();
                manager.countTypeDeclarations();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test3 = new Timer(() -> {
            try{
                DatasetManager manager = new DatasetManager();
                manager.countTypeUsage();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test4 = new Timer(() -> {
            try{
                DatasetManager manager = new DatasetManager();
                manager.refreshCopies(pm -> pm.getSpec().metrics().version() != JavaVersion.BEFORE_JAVA16);
                manager.collectDifferenceInfo();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test5 = new Timer(() -> {
            try{
                DatasetManager manager = new DatasetManager();
                //manager.refreshCopies(pm -> pm.getSpec().metrics().version() != JavaVersion.BEFORE_JAVA16);
                manager.mineRecordHistory();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test6 = new Timer(() -> {
            try{
                new TypeComparator().compareType();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        test6.run();
    }

    public static File debugFile(){
        return new File("./org/posl/test/TestSource.java");
    }

    @FunctionalInterface
    public interface IOConsumer<T>{
        public void accept(T t) throws IOException;
    }

}