package rm4j.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import rm4j.compiler.core.ParserException;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.test.ClassTotalizer;
import rm4j.test.RecordTotalizer;
import rm4j.util.SimpleCounter;

public class DatasetManager{

    private static final int DATASET_SIZE = 2000;
    private static final int NUM_OF_TIMESTAMPS = 46;

    private static final File REPOSITORY_STATUS_DIRECTORY = new File("work/repositoryStatus");
    public static final File DATASET_DIRECTORY = new File("../data/repositories");

    public static final Set<String> RECORD_USECASE_REPOSITORY_NAMES = new HashSet<>(Arrays.asList(
        "logisim-evolution",
        "cas",
        "openj9",
        "Chunky",
        "sparrow",
        "Applied-Energistics-2",
        "restheart",
        "jdbi",
        "sirix",
        "high-performance-java-persistence",
        "checkstyle",
        "sdrtrunk",
        "intellij-plugins",
        "gson",
        "stroom",
        "pcgen",
        "Configurate",
        "AsciidocFX",
        "pdfsam",
        "spring-data-elasticsearch",
        "graylog2-server",
        "CodeKatas",
        "spring-data-mongodb",
        "sejda",
        "LiveLessons", "jenetics", "CloudNet-v3", "Smithereen", "kcctl", "spring-data-commons",
        "odd-platform", "jbang", "junit5", "JustEnoughItems", "FastAsyncWorldEdit", "Bytecoder", "crate", "Signal-Server", "signal-cli", "bazel",
        "spring-cloud-gateway", "cloudsimplus", "SapMachine", "oraxen", "geckolib", "spoon", "MCreator", "dependency-track", "TechReborn", "blog-tutorials",
        "sonar-java", "hazelcast", "jabref", "WorldEdit", "ImmersivePortalsMod", "sodium-fabric", "Botania", "CompreFace", "ArchUnit", "spring-data-neo4j",
        "JetBrainsRuntime", "bnd", "Create", "Springy-Store-Microservices", "spring-data-redis", "spring-batch", "equalsverifier", "elasticsearch", "DrivenByMoss", "marquez",
        "spark"
    ));

    public static final Date[] TIMESTAMPS = ((Supplier<Date[]>) () -> {
        Date[] timestamps= new Date[NUM_OF_TIMESTAMPS];
        int year = 2019;
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

    private final RepositoryManager[] repositories = new RepositoryManager[DATASET_SIZE];
    public Integer[][] data = new Integer[DATASET_SIZE][NUM_OF_TIMESTAMPS];

    public DatasetManager(FileFilter filter) throws IOException{
        int i = 0;
        List<String> failed = new ArrayList<>();
        for (File repository : DATASET_DIRECTORY.listFiles()){
            if(!repository.getName().equals(".DS_Store")){
                if(filter.accept(repository)){
                    System.out.println("%d : %s".formatted(i+1, repository.getName()));
                    repositories[i] = readCommitManager(repository);
                    if(repositories[i] == null){
                        repositories[i] = new RepositoryManager(repository);
                        writeCommitManager(repositories[i]);
                    }
                }
                i++;
            }
        }
        if (failed.size() > 0){
            System.out.println("Failed to update following repositories, try yourself:");
            for (String s : failed){
                System.out.println(s);
            }
            throw new IOException("Failed updating some repositories.");
        }
    }

    public void collectDataOfSingleTrace(FileFilter filter) throws IOException{
        File out = new File("work/result3.csv");
        out.createNewFile();
        try(FileWriter writer = new FileWriter(out)){
            for(int i = 0; i < DATASET_SIZE; i++){
                RepositoryManager repository = repositories[i];
                if(repository != null && filter.accept(repository.repository())){
                    int count = 0;
                    String buf = "%d: %s, ".formatted(i+1, repository.repository().getName());
                    System.out.print(buf);
                    List<CommitInfo> commitTraceList = new ArrayList<>(Arrays.asList(repository.commitTrace()));
                    commitTraceList.sort((c1, c2) -> c2.date().compareTo(c1.date()));
                    int prev = -1;

                    for(int j = 1; j <= NUM_OF_TIMESTAMPS; j++){
                        int k = 0;
                        while(k < commitTraceList.size() && TIMESTAMPS[NUM_OF_TIMESTAMPS - j].compareTo(commitTraceList.get(k).date()) < 0){
                            k++;
                        }
                        if(prev != k){
                            while(k < commitTraceList.size() && !repository.checkout(commitTraceList.get(k).id())){
                                k++;
                            }
                        }
                        if(k != commitTraceList.size() && TIMESTAMPS[NUM_OF_TIMESTAMPS - j].compareTo(commitTraceList.get(k).date()) >= 0){
                            if(prev != k){
                                SimpleCounter counter = new SimpleCounter();
                                repository.createProjectUnit(t -> {
                                    if(t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                                        counter.countUp();
                                    }
                                });
                                count = counter.getCount(); 
                                prev = k;
                            }
                            buf += count;
                            System.out.print(count);
                        }else{
                            buf += "null";
                            System.out.print("null");
                        }
                        if(j != NUM_OF_TIMESTAMPS){
                            buf += ", ";
                            System.out.print(", ");
                        }else{
                            buf += "\n";
                            System.out.println();
                        }
                    }
                    writer.append(buf);
                }
            }
        }
    }

    public void collectDataFromDifference() throws IOException{
        //.csv file for statistics of all repository
        File result = new File("work/result_dif.csv");
        result.createNewFile();

        //working directory for saving previous revision of .java files
        File workingDirectory = new File("work/fileBuffer");

        //start writing to result file
        try(FileWriter resultWriter = new FileWriter(result)){
            //for each repository
            int repositoryId = 0;
            for(int i = 0; i < DATASET_SIZE; i++){
                RepositoryManager repository = repositories[i];
                if(repository != null && RECORD_USECASE_REPOSITORY_NAMES.contains(repository.repository().getName())){
                    resultWriter.write(repository.collectDifferenceData(++repositoryId, workingDirectory));
                }
            }
        }
    }

    public void collectURL() throws IOException{
        for(int i = 0; i < DATASET_SIZE; i++){
            RepositoryManager repository = repositories[i];
            if(i == 260 || i == 1020 || i == 1294){
                System.out.println(repository.repository());
                if(repository != null){
                    System.out.println("git clone "+repository.getFetchURL());
                }
            }
        }
    }

    public void collectDataOfAllTypes(FileFilter filter) throws IOException{
        final SimpleCounter[] counters = new SimpleCounter[5];
        for(int i = 0; i < counters.length; i++){
            counters[i] = new SimpleCounter();
        }
        for(int i = 0; i < DATASET_SIZE; i++){
            RepositoryManager repository = repositories[i];
            if(repository != null && filter.accept(repository.repository())){
                final SimpleCounter[] subCounters = new SimpleCounter[5];
                for(int j = 0; j < subCounters.length; j++){
                    subCounters[j] = new SimpleCounter();
                }
                int j = 0;
                List<CommitInfo> commitTraceList = new ArrayList<>(Arrays.asList(repository.commitTrace()));
                commitTraceList.sort((c1, c2) -> c2.date().compareTo(c1.date()));
                while(j < commitTraceList.size() && !repository.checkout(commitTraceList.get(j).id())){
                    System.out.println("%s - %s".formatted(commitTraceList.get(j).date(), commitTraceList.get(j).id().toString(16)));
                    j++;
                }
                repository.createProjectUnit(t -> {
                    if(t instanceof ClassTree c){
                        int id = switch(c.declType()){
                            case CLASS -> 0;
                            case ENUM -> 1;
                            case INTERFACE -> 2;
                            case ANNOTATION_INTERFACE -> 3;
                            case RECORD -> 4;
                            default -> throw new ParserException("");
                        };
                        counters[id].countUp();
                        subCounters[id].countUp();
                    }
                });
                String counts = "";
                for(int k = 0; k < subCounters.length; counts += ", "){
                    counts += subCounters[k++].getCount();
                    if(k == subCounters.length){
                        break;
                    }
                }
                System.out.println("%d: %s: %s".formatted(i+1, repository.repository().getName(), counts));
            }
        }
        String counts = "";
        for(int k = 0; k < counters.length; counts += ", "){
            counts += counters[k++].getCount();
            if(k == counters.length){
                break;
            }
        }
        System.out.println("total: %s".formatted(counts));   
    }

    public void collectDataOfRecords(FileFilter filter) throws IOException{
        ClassTotalizer totalizer = new ClassTotalizer();
        final SimpleCounter counter = new SimpleCounter();
        File workingDir = new File("work/immutableClassData");
        workingDir.mkdir();
        for(int i = 0; i < 7; i++){
            new File(workingDir, i + "").mkdir();
        }
        for(int i = 0; i < DATASET_SIZE; i++){
            RepositoryManager repository = repositories[i];
            if(repository != null && filter.accept(repository.repository())){
                final SimpleCounter subCounter = new SimpleCounter();
                int j = 0;
                List<CommitInfo> commitTraceList = new ArrayList<>(Arrays.asList(repository.commitTrace()));
                commitTraceList.sort((c1, c2) -> c2.date().compareTo(c1.date()));
                while(j < commitTraceList.size() && !repository.checkout(commitTraceList.get(j).id())){
                    System.out.println("%s - %s".formatted(commitTraceList.get(j).date(), commitTraceList.get(j).id().toString(16)));
                    j++;
                }
                repository.createProjectUnit(t -> {
                    if(t instanceof ClassTree c){
                        totalizer.collectInformation(c);
                        if(c.declType() == DeclarationType.CLASS){
                            counter.countUp();
                            subCounter.countUp();
                        }
                    }
                });
                System.out.println("%d: %s: %d".formatted(i+1, repository.repository().getName(), subCounter.getCount()));
            }
        }
        System.out.println("total: %d".formatted(counter.getCount()));
        totalizer.writeData();
    }
    
    public boolean writeCommitManager(RepositoryManager commitManager) throws IOException{
        File out = new File(REPOSITORY_STATUS_DIRECTORY + "/" + commitManager.repository().getName() + ".ser");
        out.createNewFile();
        try(var writer = new ObjectOutputStream(new FileOutputStream(out))){
            writer.writeObject(commitManager);
            return true;
        }catch(IOException e){
            out.delete();
            throw e;
        }
    }

    public RepositoryManager readCommitManager(File repository) throws IOException{
        File in = new File(REPOSITORY_STATUS_DIRECTORY + "/" + repository.getName() + ".ser");
        if(!in.exists()){
            return null;
        }
        try(var reader = new ObjectInputStream(new FileInputStream(in))){
            return (RepositoryManager)reader.readObject();
        }catch(ClassNotFoundException e){
            return null;
        }
    }

}