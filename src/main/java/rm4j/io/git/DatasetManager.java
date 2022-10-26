package rm4j.io.git;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.util.SimpleCounter;

public class DatasetManager{

    private static final int DATASET_SIZE = 2000;
    private static final int CHECKPOINTS_SIZE = 31;
    public static final File REPOSITORIES = new File("../data/repositories");

    public static final List<String> RECORD_USECASE_REPOSITORY_NAMES = new ArrayList<>(Arrays.asList(
        "logisim-evolution", "cas", "openj9", "Chunky", "Sponge", "sparrow", "Applied-Energistics-2", "restheart", "JPlag", "sirix", 
        "high-performance-java-persistence", "checkstyle", "sdrtrunk", "gson", "stroom", "hellokoding-courses", "pcgen", "Configurate", "activej", "AsciidocFX",
        "pdfsam", "spring-data-elasticsearch", "CodeKatas", "spring-data-mongodb", "LiveLessons", "jenetics", "OpenLegislation", "CloudNet-v3", "Minestom", "spring-data-commons",
        "CatServer", "jbang", "junit5", "JustEnoughItems", "CraftBook", "Bytecoder", "crate", "Signal-Server", "signal-cli", "bazel",
        "jetty.project", "cloudsimplus", "FastCSV", "SapMachine", "oraxen", "geckolib", "spoon", "MCreator", "airbyte", "dependency-track",
        "TechReborn", "blog-tutorials", "sonar-java", "hazelcast", "jabref", "ImmersivePortalsMod", "sodium-fabric", "Botania", "ArchUnit", "JetBrainsRuntime",
        "bnd", "Create", "Springy-Store-Microservices", "spring-batch", "equalsverifier", "elasticsearch", "DrivenByMoss", "marquez", "xstream"
    ));

    public static final Date[] CHECKPOINTS = ((Supplier<Date[]>) () ->{
        Date[] checkpoints = new Date[CHECKPOINTS_SIZE];
        int year = 2020;
        int month = 4;
        for (int i = 0; i < CHECKPOINTS_SIZE; i++){
            checkpoints[i] = new Date(year, month++, 1);
            if (month > 12){
                year++;
                month = 1;
            }
        }
        return checkpoints;
    }).get();

    private final GitCommitManager[] dataManagers = new GitCommitManager[RECORD_USECASE_REPOSITORY_NAMES.size()];
    public Integer[][] data = new Integer[CHECKPOINTS_SIZE][RECORD_USECASE_REPOSITORY_NAMES.size()];

    public DatasetManager() throws IOException{
        int i = 0;
        List<String> failed = new ArrayList<>();
        for (String repositoryName : RECORD_USECASE_REPOSITORY_NAMES){
            dataManagers[i++] = new GitCommitManager(new File(REPOSITORIES.toString() + "/" + repositoryName));
            //dataManagers.fetch
        }
        if (failed.size() > 0){
            System.out.println("Failed to update following repositories, try yourself:");
            for (String s : failed){
                System.out.println(s);
            }
            throw new IOException("Failed updating some repositories.");
        }
    }

    public void collectData() throws IOException{
        for (int i = 0; i < CHECKPOINTS_SIZE; i++){
            System.out.println("Status = %s".formatted(CHECKPOINTS[i]));
            for (int j = 0; j < RECORD_USECASE_REPOSITORY_NAMES.size(); j++){
                if (dataManagers[j].checkout(CHECKPOINTS[i])){
                    System.out.println("%d: checkout %s".formatted(j, dataManagers[j].repository().getName()));
                    final SimpleCounter counter = new SimpleCounter();
                    dataManagers[j].createProjectUnit(t ->{
                        if (t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                            counter.countUp();
                        }
                    });
                    data[i][j] = counter.getCount();
                }else{
                    System.out.println("%d: checkout of %s failed, commit length = %d".formatted(j,
                            dataManagers[j].repository().getName(), dataManagers[j].commits().length));
                    data[i][j] = (i == 0) ? null : data[i - 1][j];
                }
            }
        }
        File out = new File("work/result.csv");
        out.createNewFile();
        try (FileWriter writer = new FileWriter(out)){
            for (int j = 0; j < RECORD_USECASE_REPOSITORY_NAMES.size(); j++){
                String line = dataManagers[j].repository().getName() + ", ";
                for(int i = 0; i < CHECKPOINTS_SIZE; i++){
                    line += data[i][j] + ((i == CHECKPOINTS_SIZE - 1)? "\n" : ", ");
                }
                writer.append(line);
            }
        }

    }

    public static void getCommitInfo(String repositoryName) throws IOException{
        GitCommitManager commitManager = new GitCommitManager(new File(REPOSITORIES.toString() + "/" + repositoryName));
        for(CommitInfo info : commitManager.commits()){
            System.out.println(info);
        }
    }

    public static void traceAllCommits(String repositoryName, Date since, Date until) throws IOException{
        GitCommitManager commitManager = new GitCommitManager(new File(REPOSITORIES.toString() + "/" + repositoryName));
        for(CommitInfo info : commitManager.commits()){
            if(since.compareTo(info.date()) <= 0 && info.date().compareTo(until) < 0){
                BigInteger id = info.id();
                if(commitManager.checkout(id)){
                    final SimpleCounter counter = new SimpleCounter();
                    commitManager.createProjectUnit(t ->{
                        if (t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                            counter.countUp();
                        }
                    });
                    System.out.println("date = %s, id = %s, records = %d".formatted(info.date(), id.toString(16), counter.getCount()));
                }else{
                    System.out.println("date = %s, id = %s, checkout failed".formatted(info.date(), id.toString(16)));
                }
            }
        }
    }

    public static void test(){
        Integer[] testResult = new Integer[CHECKPOINTS_SIZE];
        try{
            GitCommitManager testRepository = new GitCommitManager(new File(REPOSITORIES.toString() + "/CatServer"));
            for (int i = 0; i < CHECKPOINTS_SIZE; i++){
                System.out.println("Status = %s".formatted(CHECKPOINTS[i]));
                if (testRepository.checkout(CHECKPOINTS[i])){
                    final SimpleCounter counter = new SimpleCounter();
                    testRepository.createProjectUnit(t ->{
                        if (t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                            counter.countUp();
                        }
                    });
                    testResult[i] = counter.getCount();
                }else{
                    testResult[i] = (i == 0) ? null : testResult[i-1];
                }
            }
            for(Integer result : testResult){
                System.out.print("%d, ".formatted(result));
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }
}
