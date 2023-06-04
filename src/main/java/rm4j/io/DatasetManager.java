package rm4j.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import rm4j.io.Metrics.JavaVersion;
import rm4j.util.functions.CEConsumer;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.InstanceOfTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;

public class DatasetManager implements FileManager{

    private static final int DATASET_SIZE = 2000;
    private static final int NUM_OF_TIMESTAMPS = 38;

    public static final File DATASET_DIRECTORY = new File("../data_original/repositories");

    public static final Date[] TIMESTAMPS = ((Supplier<Date[]>) () -> {
        Date[] timestamps= new Date[NUM_OF_TIMESTAMPS];
        int year = 2020;
        int month = 4;
        for (int i = 0; i < NUM_OF_TIMESTAMPS; i++){
            timestamps[i] = new Date(year, month++, 1);
            if (month > 12){
                year++;
                month = 1;
            } 
        }
        return timestamps;
    }).get();

    private final ProjectManager[] repositories = new ProjectManager[DATASET_SIZE];

    public DatasetManager() throws IOException{
        for(int i = 0; i < DATASET_SIZE; i++){
            repositories[i] = ProjectManager.deserialize(i+1);
        }
    }

    public void getMetrics() throws IOException{
        String labels = "repository_name, age, number_of_commits, number_of_authors, number_of_java_files, version";
        Metrics[] metrics = new Metrics[DATASET_SIZE];
        List<Metrics> list1 = new LinkedList<>();
        List<Metrics> list2 = new LinkedList<>();
        for(int i = 0; i < DATASET_SIZE; i++){
            System.out.println("Measuring metrics of rep%d:%s...".formatted(i+1, repositories[i].getProjectName()));
            metrics[i] = repositories[i].measureMetrics();
            if(metrics[i].version() != JavaVersion.BEFORE_JAVA16){
                list1.add(metrics[i]);
                if(metrics[i].version() == JavaVersion.HAS_RECORDS){
                    list2.add(metrics[i]);
                }
            }
        }

        writeCSV(labels, metrics, new File("work/dataset_spec/all_repository_metrics.csv"));
        writeCSV(labels, list1, new File("work/dataset_spec/Java16_repository_metrics.csv"));
        writeCSV(labels, list2, new File("work/dataset_spec/record_repository_metrics.csv"));
    }

    public void countTypeDeclarations() throws IOException{
        record Tuple(String repName, int[] data) implements CSVTuple{

            Tuple{if(data.length != 5) throw new IllegalArgumentException();}

            @Override
            public String toCSVLine(){
                String ret = repName;
                for(int i = 0; i < 5; i++){
                    ret += ", " + data[i];
                }
                return ret;
            }

        }
        String labels = "repository_name, classes, enums, interfaces, annotation_interfaces, records";
        Tuple[] data = new Tuple[DATASET_SIZE];
        List<Tuple> list1 = new LinkedList<>();
        List<Tuple> list2 = new LinkedList<>();

        for(int i = 0; i < DATASET_SIZE; i++){
            System.out.println("Counting type declarations of rep%d:%s...".formatted(i+1, repositories[i].getProjectName()));
            int[] counts = repositories[i].countTypeDeclarations();
            data[i] = new Tuple(repositories[i].getProjectName(), Arrays.copyOf(counts, 5));
            switch(counts[5]){
                case 3: list2.add(data[i]);
                // fall through
                case 1: list1.add(data[i]);
            }
        }

        writeCSV(labels, data, new File("work/rq1-1/type_declarations_A.csv"));
        writeCSV(labels, list1, new File("work/rq1-1/type_declarations_B.csv"));
        writeCSV(labels, list2, new File("work/rq1-1/type_declarations_C.csv"));
    }

    public void writeCSV(String labels, CSVTuple[] data, File path) throws IOException{
        path.delete();
        path.createNewFile();
        try(FileWriter writer = new FileWriter(path)){
            writer.append(labels + "\n");
            for(CSVTuple t : data){
                writer.append(t.toCSVLine() + "\n");
            }
        }
    }

    public void writeCSV(String labels, List<? extends CSVTuple> data, File path) throws IOException{
        writeCSV(labels, data.toArray(CSVTuple[]::new), path);
    }

}