package rm4j.io.git;

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

import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.util.SimpleCounter;

public class DatasetManager{

    private static final int DATASET_SIZE = 2000;
    private static final int NUM_OF_TIMESTAMPS = 31;

    private static final File REPOSITORY_STATUS_DIRECTORY = new File("work/repositoryStatus");
    public static final File DATASET_DIRECTORY = new File("../data/repositories");

    public static final List<String> RECORD_USECASE_REPOSITORY_NAMES = new ArrayList<>(Arrays.asList(
        "logisim-evolution", "cas", "openj9", "Chunky", "Sponge", "sparrow", "Applied-Energistics-2", "restheart", "JPlag", "sirix", 
        "high-performance-java-persistence", "checkstyle", "sdrtrunk", "gson", "stroom", "hellokoding-courses", "pcgen", "Configurate", "activej", "AsciidocFX",
        "pdfsam", "spring-data-elasticsearch", "CodeKatas", "spring-data-mongodb", "LiveLessons", "jenetics", "OpenLegislation", "CloudNet-v3", "Minestom", "spring-data-commons",
        "CatServer", "jbang", "junit5", "JustEnoughItems", "CraftBook", "Bytecoder", "crate", "Signal-Server", "signal-cli", "bazel",
        "jetty.project", "cloudsimplus", "FastCSV", "SapMachine", "oraxen", "geckolib", "spoon", "MCreator", "airbyte", "dependency-track",
        "TechReborn", "blog-tutorials", "sonar-java", "hazelcast", "jabref", "ImmersivePortalsMod", "sodium-fabric", "Botania", "ArchUnit", "JetBrainsRuntime",
        "bnd", "Create", "Springy-Store-Microservices", "spring-batch", "equalsverifier", "elasticsearch", "DrivenByMoss", "marquez", "xstream"
    ));

    public static final Date[] TIMESTAMPS = ((Supplier<Date[]>) () ->{
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

    private final RepositoryManager[] repositories = new RepositoryManager[DATASET_SIZE];
    public Integer[][] data = new Integer[DATASET_SIZE][NUM_OF_TIMESTAMPS];

    public DatasetManager() throws IOException{
        int i = 0;
        List<String> failed = new ArrayList<>();
        for (File repository : DATASET_DIRECTORY.listFiles()){
            if(!repository.getName().equals(".DS_Store")){
                System.out.println("%d : %s".formatted(i+1, repository.getName()));
                repositories[i] = readCommitManager(repository);
                if(repositories[i] == null){
                    repositories[i] = new RepositoryManager(repository);
                    writeCommitManager(repositories[i]);
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
        File out = new File("work/result.csv");
        out.createNewFile();
        try(FileWriter writer = new FileWriter(out)){
            for(int i = 0; i < DATASET_SIZE; i++){
                RepositoryManager repository = repositories[i];
                if(filter.accept(repository.repository())){
                    Integer count = -1;
                    String buf = "%d: %s, ".formatted(i+1, repository.repository().getName());
                    System.out.println(repository.repository().getName());
                    CommitInfo[] trace = repository.commitTrace();
                    int k = 0;
                    for(int j = 0; j < NUM_OF_TIMESTAMPS; j++){
                        while(k < trace.length && TIMESTAMPS[j].compareTo(trace[k].date()) < 0){
                            k++;
                            count = -1;
                        }
                        if(k < trace.length){
                            if(count == -1){
                                repository.checkout(trace[k].id());
                                System.out.println(trace[k].date());
                                SimpleCounter counter = new SimpleCounter();
                                repository.createProjectUnit(t -> {
                                    if(t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                                        counter.countUp();
                                    }
                                });
                            }
                            buf += count;
                        }else{
                            buf += "null";
                        }
                        if(j != NUM_OF_TIMESTAMPS - 1){
                            buf += ", ";
                        }
                    }
                    writer.append(buf);
                }
            }
        }
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
