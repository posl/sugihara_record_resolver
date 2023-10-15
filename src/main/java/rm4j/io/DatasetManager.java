package rm4j.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import rm4j.io.Metrics.JavaVersion;
import rm4j.test.Test;

public class DatasetManager implements FileManager{

    private static final int DATASET_SIZE;
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

    static{
        DATASET_SIZE = Test.DATASET_SIZE;
    }

    private final ProjectManager[] repositories = new ProjectManager[DATASET_SIZE];

    public DatasetManager() throws IOException{
        for(int i = 0; i < DATASET_SIZE; i++){
            repositories[i] = new ProjectManager(i+1);
        }
    }

    public void refreshCopies(Predicate<? super ProjectManager> filter) throws IOException{
        Process p = Runtime.getRuntime().exec("bash shell/refresh.sh");
        try{
            p.waitFor();
        }catch(InterruptedException e){
            throw new IOException(e);
        }
        // for(var project : repositories){
        //     if(filter.test(project)){
        //         project.refreshCopies();
        //     }
        // }
    }

    public void getMetrics() throws IOException{
        String labels = "repository_name,age,number_of_commits,number_of_authors,number_of_java_files,version";
        ProjectSpec[] specs = Stream.of(repositories).map(ProjectManager::getSpec).toArray(ProjectSpec[]::new);
        writeCSV(labels, specs, new File("out/dataset_spec/all_repository_metrics.csv"));
        writeCSV(labels, Stream.of(specs).filter(spec -> spec.metrics().version() != JavaVersion.BEFORE_JAVA16).toList(), new File("out/dataset_spec/Java16_repository_metrics.csv"));
        writeCSV(labels, Stream.of(specs).filter(spec -> spec.metrics().version() != JavaVersion.HAS_RECORDS).toList(), new File("out/dataset_spec/record_repository_metrics.csv"));
    }

    public void collectDifferenceInfo() throws IOException{
        String labels = "repository_name,with_new_files,to_record_conversion,added_to_file,with_deleted_files,to_class_conversion,removed_from_file";
        List<String> data = new LinkedList<>();
        for(ProjectManager project : repositories){
            if(project.isCompatibleWithJava16()){
                data.add(project.collectDifferenceData());
            }
        }
        recordDataInCSV(labels, data, new File("out/rq2-1/diffs/diff_summerize.csv"));
    }

    public void mineRecordHistory() throws IOException{
        String labels = "";
        Date since = new Date(2020, 4, 1);
        for(Date date = new Date(2023, 6, 1); date.compareTo(since) >= 0; date = date.previousMonth()){
            labels = "," + date.toDateString() + labels;   
        }
        labels = "repository_name" + labels;
        List<String> data = new LinkedList<>();
        for(ProjectManager project : repositories){
            if(project.isCompatibleWithJava16()){
                data.add(project.mineRecordHistory());
            }
        }
        recordDataInCSV(labels, data, new File("out/rq1-1/record_history.csv"));
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
        String labels = "repository_name,classes,enums,interfaces,annotation_interfaces,records";
        Tuple[] data = new Tuple[DATASET_SIZE];
        List<Tuple> list1 = new LinkedList<>();
        List<Tuple> list2 = new LinkedList<>();

        for(int i = 0; i < DATASET_SIZE; i++){
            System.out.println("Counting type declarations of rep%d:%s...".formatted(i+1, repositories[i].getProjectName()));
            data[i] = new Tuple(repositories[i].getProjectName(), repositories[i].countTypeDeclarations());
            if(repositories[i].isCompatibleWithJava16()){
                list1.add(data[i]);
            }
            if(repositories[i].hasRecords()){
                list2.add(data[i]);
            }
        }

        writeCSV(labels, data, new File("out/rq1-1/type_declarations_A.csv"));
        writeCSV(labels, list1, new File("out/rq1-1/type_declarations_B.csv"));
        writeCSV(labels, list2, new File("out/rq1-1/type_declarations_C.csv"));
    }

    public void countTypeUsage() throws IOException{
        TypeDeclarationData classData = new TypeDeclarationData();
        TypeDeclarationData recordData = new TypeDeclarationData();
        APIResolver typeResolver = APIResolver.deserialize();
        for(int i = 0; i < DATASET_SIZE; i++){
            if(repositories[i].isCompatibleWithJava16()){
                System.out.println("Analyzing type declarations of rep%d:%s...".formatted(i+1, repositories[i].getProjectName()));
                repositories[i].countTypeUsage(classData, recordData, typeResolver);
            }
        }
        String[] labelsList = {"num_of_fields", "field_type", "num_of_interfaces", "interface_type", "num_of_methods"};
        for(int i = 0; i < 5; i++){
            writeCSV("%s, frequency".formatted(labelsList[i]),
                        classData.enumerateRankers().get(i).getRanking(),
                        new File("out/rq1-2/classes/%s.csv".formatted(labelsList[i])));
        }
        for(int i = 0; i < 5; i++){
            writeCSV("%s, frequency".formatted(labelsList[i]),
                        recordData.enumerateRankers().get(i).getRanking(),
                        new File("out/rq1-2/records/%s.csv".formatted(labelsList[i])));
        }
    }

    public void mineClassInfo() throws IOException{
        String labels = "repository_name,all_classes,has_non_final_fields,extends_other_class,has_prohibited_modifiers,has_instrance_initializer,has_inproper_canonical_constructor,has_get_prefixed_getters,has_effective_getters,record_format_classes";
        List<String> data = new LinkedList<>();
        for(ProjectManager project : repositories){
            if(project.isCompatibleWithJava16() && !project.hasRecords()){
                data.add(project.collectClassData());
            }
        }
        recordDataInCSV(labels, data, new File("out/rq2-2/class_data.csv"));
    }

    public void mineClassesAndExpressions() throws IOException{
        String labels = "repository_name,classes,enum,records,interfaces,annotations,all_reference_types,simple_assignment,method_arguments,both_expressions";
        List<String> data = Arrays.stream(repositories)
                                .parallel()
                                .map(ProjectManager::collectClassesAndExpressions)
                                .toList();
        recordDataInCSV(labels, data, new File("out/rq1-1/expressions.csv"));
    }

    public void mineAccessors() throws IOException{
        String labels = "repository_name,all_effective_getters,get_prefixed_getters,record_format_getters";
        List<String> data = Arrays.stream(repositories)
                                .parallel()
                                .map(ProjectManager::collectAccessorsInfo)
                                .toList();
        recordDataInCSV(labels, data, new File("out/rq2-2/accessors.csv"));
    }

    public void writeCSV(String labels, CSVTuple[] data, File path) throws IOException{
        if(!path.getParentFile().exists()){
            path.getParentFile().mkdirs();
        }
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