package rm4j.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.InstanceOfTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.io.Metrics.JavaVersion;
import rm4j.util.functions.CEConsumer;

public class ProjectManager implements Serializable, FileManager{

    private static final long serialVersionUID = 0xF4D9F068C644D8DCL;

    private static final Date HEAD_COMMIT_STAMP = new Date(2023, 6, 1);
    private static final File INSTANCES_PARENT = new File("work/manager_instances");
    private static final File PROJECTS_PARENT = new File("../data_original/repositories");

    private final File project;
    private final File original;
    private final File copied;

    private final CommitTrace commitTrace;

    public static ProjectManager deserialize(int number) throws IOException{
        File instancePath = new File(INSTANCES_PARENT, "rep%d.ser".formatted(number));
        if(instancePath.exists()){
            try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(instancePath))){
                return (ProjectManager)in.readObject();
            }catch(ClassNotFoundException | ClassCastException e){
                e.printStackTrace();
            }
        }
        return new ProjectManager(new File(PROJECTS_PARENT, "rep%d".formatted(number)));
    }

    public ProjectManager(File project) throws IOException{
        System.out.println("initializing %s ...".formatted(project.getName()));
        this.project = project;
        File dir = new File(project,"original");
        if(dir.listFiles(f -> f.isDirectory()).length == 1){
            this.original = dir.listFiles(f -> f.isDirectory())[0];
        }else{
            throw new IOException("Original repository doesn't exist.");
        }
        this.commitTrace = new CommitTrace(cmd -> getStreamReader(cmd, original));
        CommitInfo headCommitInfo = commitTrace.revert(HEAD_COMMIT_STAMP);
        if(headCommitInfo != null){
            run("git checkout %s -f".formatted(headCommitInfo.id()), original);   
        }
        this.copied = new File(project, "copied/" + original.getName());
        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(INSTANCES_PARENT, project.getName()+".ser")))){
            out.writeObject(this);
        }
    }

    public Metrics measureMetrics() throws IOException{
        double age;
        int commits = 0;
        int authors = 0;
        int[] javaFiles = {0};
        JavaVersion[] version = {JavaVersion.BEFORE_JAVA16};
        Date date = commitTrace.getCommitInfo(getStreamReader("git log --reverse", original)).date();
        age = 12 * (HEAD_COMMIT_STAMP.year() - date.year() - 1)
                + (HEAD_COMMIT_STAMP.month() + 11 - date.month())
                + (HEAD_COMMIT_STAMP.date() + 30 - date.date())/30.0;
        try(BufferedReader reader = new BufferedReader(new FileReader(new File(project, "commits.txt")))){
            String buf;
            while((buf = reader.readLine()) != null){
                commits += Integer.parseInt(buf.split("[^0-9]+")[1]);
                authors++;
            }
        }
        forEachFiles(original, f -> {
            if(f.getName().endsWith(".java")){
                javaFiles[0]++;
            }
        });
        new ProjectUnit(original, t -> {
            if(t instanceof ClassTree c){
                if(c.declType() == DeclarationType.RECORD && version[0] != JavaVersion.HAS_RECORDS){
                    version[0] = JavaVersion.HAS_RECORDS;
                }
                if((c.modifiers().getModifiers().contains(ModifierKeyword.SEALED)
                    || c.modifiers().getModifiers().contains(ModifierKeyword.NON_SEALED))
                    && version[0] == JavaVersion.BEFORE_JAVA16){
                        version[0] = JavaVersion.AFTER_JAVA16;
                }
            }else if(t instanceof InstanceOfTree i){
                if(i.pattern() != null && version[0] == JavaVersion.BEFORE_JAVA16){
                    version[0] = JavaVersion.AFTER_JAVA16;
                }
            }
        });
        return new Metrics(getProjectName(), age, commits, authors, javaFiles[0], version[0]);
    }

    public int[] countTypeDeclarations() throws IOException{
        int[] data = new int[6];
        new ProjectUnit(original, t -> {
            if(t instanceof ClassTree c){
                switch(c.declType()){
                    case CLASS -> data[0]++;
                    case ENUM -> data[1]++;
                    case INTERFACE -> data[2]++;
                    case ANNOTATION_INTERFACE -> data[3]++;
                    case RECORD -> {
                        data[4]++;
                        data[5] |= 0b11;
                    }
                    default -> {}
                }
                if((c.modifiers().getModifiers().contains(ModifierKeyword.SEALED)
                    || c.modifiers().getModifiers().contains(ModifierKeyword.NON_SEALED))){
                    data[5] |= 1;
                }
            }else if(t instanceof InstanceOfTree it){
                if(it.pattern() != null){
                    data[5] |= 1;
                }
            }
        });
        return data;
    }

    public void query(CEConsumer<Tree> query){
        new ProjectUnit(original, query);
    }

    public void queryAll(Collection<CEConsumer<Tree>> queries){
        new ProjectUnit(original, t -> {
            for(var query : queries){
                query.accept(t);
            }
        });
    }

    public boolean refreshCopies(){
        System.out.println("refreshing %s ...".formatted(copied));
        deleteAll(copied);
        return copyFiles(original, copied);
    }

    public String getFetchURL() throws IOException{
        try(var reader = getStreamReader("git remote show origin", original)){
            String buf;
            while((buf = reader.readLine()) != null && !buf.startsWith("    ")){
                if(buf.startsWith("  Fetch URL: ")){
                    return buf.split("  Fetch URL: ")[1];
                }
            }
        }
        return "Connection Error";
    }

    public boolean checkout(BigInteger id) throws IOException{
        return run("git checkout %s -f --'*.java'".formatted(id.toString(16)), copied);
    }

    public List<File> getChangedFiles() throws IOException{
        List<File> changedFiles = new ArrayList<>();
        try(var reader = getStreamReader("git diff HEAD~ --name-only", copied)){
            String buf;
            while((buf = reader.readLine()) != null){
                changedFiles.add(new File("%s/%s".formatted(copied, buf)));
            }
        }
        return changedFiles;
    }
    
    private boolean run(String cmd, File dir) throws IOException{
        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        try{
            p.waitFor();
        }catch (InterruptedException e){
            throw new IOException(e);
        }
        if (p.exitValue() == 0){
            return true;
        }else{
            String line;
            while ((line = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine()) != null){
                System.out.println(line);
            }
            return false;
        }
    }

    private InputStream getStream(String command, File dir) throws IOException{
        return Runtime.getRuntime().exec(command, null, dir).getInputStream();
    }

    private BufferedReader getStreamReader(String command, File dir) throws IOException{
        return new BufferedReader(new InputStreamReader(getStream(command, dir)));
    }

    public CommitInfo[] commitTrace(){
        return commitTrace.commits();
    }

    public File projectDirectory(){
        return project;
    }

    public String getProjectName(){
        return original.getName();
    }
}
