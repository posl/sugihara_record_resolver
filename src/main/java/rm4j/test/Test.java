package rm4j.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

        Timer test7 = new Timer(() -> {
            try{
                new DatasetManager().mineClassInfo();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test8 = new Timer(() -> {
            try{
                new TypeComparator().getRecordToClassConversion();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test9 = new Timer(() -> {
            try{
                new DatasetManager().mineClassesAndExpressions();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        Timer test10 = new Timer(() -> {
            try{
                new DatasetManager().mineAccessors();
            }catch(IOException e){
                e.printStackTrace();
            }
        });

        if(args.length > 0){
            switch(args[0]){
                case "step1" -> test1.run();
                case "step2" -> test2.run();
                case "step3" -> test3.run();
                case "step4" -> test4.run();
                case "step5" -> test5.run();
                case "step6" -> test6.run();
                case "step7" -> test7.run();
                case "step8" -> test8.run();
                case "step9" -> test9.run();
                case "step10" -> test10.run();
            }
        }
    }

    public static File debugFile(){
        return new File("./org/posl/test/TestSource.java");
    }

    public static boolean sweep(File file){
        if(file.isDirectory()){
            return Arrays.stream(file.listFiles()).allMatch(Test::sweep);
        }else{
            if(file.getName().equals(".DS_Store")){
                return file.delete();
            }
            return true;
        }
    }

    @FunctionalInterface
    public interface IOConsumer<T>{
        public void accept(T t) throws IOException;
    }

}