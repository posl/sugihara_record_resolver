package rm4j.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import rm4j.io.DatasetManager;

public class Test{

    public static File debug = new File("./");

    public static void main(String[] args){
        Timer test1 = new Timer(() -> {
            try{
                FileFilter filter = f -> DatasetManager.RECORD_USECASE_REPOSITORY_NAMES.contains(f.getName());
                DatasetManager manager = new DatasetManager(filter);
                //DatasetManager manager = new DatasetManager(f -> DatasetManager.RECORD_USECASE_REPOSITORY_NAMES.contains(f.getName()));
                manager.collectDataOfSingleTrace(filter);
                //manager.collectDataFromDifference();
            }catch(IOException e){
                System.out.println(e);
            }
        });
        Timer test2 = new Timer(() -> {
            try{
                FileFilter filter = f -> DatasetManager.RECORD_USECASE_REPOSITORY_NAMES.contains(f.getName());
                DatasetManager manager = new DatasetManager(filter);
                manager.collectDataOfRecords(filter);
            }catch(IOException e){
                System.out.println(e);
            }
        });
        Timer test3 = new Timer(() -> {
            new RecordComparator().compareRecords();
        });
        Timer test4 = new Timer(() -> {
            try{
                FileFilter filter = f -> DatasetManager.RECORD_USECASE_REPOSITORY_NAMES.contains(f.getName());
                DatasetManager manager = new DatasetManager(filter);
                manager.collectDataOfAllTypes(filter);
            }catch(IOException e){
                System.out.println(e);
            }
        });
        Timer test5 = new Timer(() -> {
            try{
                FileFilter filter = f -> true;
                DatasetManager manager = new DatasetManager(filter);
                manager.collectURL();
            }catch(IOException e){
                System.out.println(e);
            }
        });
        test5.run();
    }

    public static File debugFile(){
        return new File("./org/posl/test/TestSource.java");
    }

    @FunctionalInterface
    public interface IOConsumer<T>{
        public void accept(T t) throws IOException;
    }

}
