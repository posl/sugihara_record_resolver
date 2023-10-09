package rm4j.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.AssignmentTree;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.InstanceOfTree;
import rm4j.compiler.tree.MethodInvocationTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.NewClassTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.TypeTree;
import rm4j.compiler.tree.VariableTree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.io.Metrics.JavaVersion;
import rm4j.util.SimpleCounter;
import rm4j.util.functions.CEConsumer;

public class ProjectManager implements FileManager{

    private static final Date HEAD_COMMIT_STAMP = new Date(2023, 6, 1);
    private static final File INSTANCES_PARENT = new File("out/project_specs");
    private static final File PROJECTS_PARENT = new File("../data_original/repositories");

    private final ProjectSpec spec;

    public ProjectManager(int number) throws IOException{
        spec = deserializeSpec(number);
        if(!copied().exists()){
            refreshCopies();
        }
    }

    private ProjectSpec deserializeSpec(int number) throws IOException{
        File instancePath = new File(INSTANCES_PARENT, "rep%d.ser".formatted(number));
        if(instancePath.exists()){
            try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(instancePath))){
                return (ProjectSpec)in.readObject();
            }catch(ClassCastException | ClassNotFoundException e){
                throw new IOException(e);
            }
        }else{
            File project = new File(PROJECTS_PARENT, "rep%d".formatted(number));
            File dir = new File(project, "original");
            if(dir.listFiles(f -> f.isDirectory()).length == 1){
                File original = dir.listFiles(f -> f.isDirectory())[0];
                File copied = new File(project, "original/" + original.getName());
                ProjectSpec spec = new ProjectSpec(project, original, copied, new CommitTrace(cmd -> getStreamReader(cmd, original)), measureMetrics(project, original));
                try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(INSTANCES_PARENT, project.getName()+".ser")))){
                    out.writeObject(spec);
                }
                return spec;
            }else{
                throw new IOException("Repository does not exist.");
            }
        }
    }

    private Metrics measureMetrics(File project, File repository) throws IOException{
        double age;
        int commits = 0;
        int authors = 0;
        int[] javaFiles = {0};
        JavaVersion[] version = {JavaVersion.BEFORE_JAVA16};
        Date date = CommitTrace.getCommitInfo(getStreamReader("git log --reverse", repository)).date();
        Date headStamp = CommitTrace.getCommitInfo(getStreamReader("git log", repository)).date();
        age = 12 * (headStamp.year() - date.year() - 1)
                + (headStamp.month() + 11 - date.month())
                + (headStamp.date() + 30 - date.date())/30.0;
        try(BufferedReader reader = new BufferedReader(new FileReader(new File(project, "commits.txt")))){
            String buf;
            while((buf = reader.readLine()) != null){
                commits += Integer.parseInt(buf.split("[^0-9]+")[1]);
                authors++;
            }
        }
        forEachFiles(repository, f -> {
            if(f.getName().endsWith(".java")){
                javaFiles[0]++;
            }
        });
        new ProjectUnit(repository, t -> {
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
        return new Metrics(age, commits, authors, javaFiles[0], version[0]);
    }

    public int[] countTypeDeclarations() throws IOException{
        int[] data = new int[5];
        new ProjectUnit(original(), t -> {
            if(t instanceof ClassTree c){
                switch(c.declType()){
                    case CLASS -> data[0]++;
                    case ENUM -> data[1]++;
                    case INTERFACE -> data[2]++;
                    case ANNOTATION_INTERFACE -> data[3]++;
                    case RECORD -> data[4]++;
                    default -> {}
                }
            }
        });
        return data;
    }

    public void countTypeUsage(TypeDeclarationData classData, TypeDeclarationData recordData, APIResolver typeResolver) throws IOException{
        final JavaCompiler compiler = new JavaCompiler();
        final PropertyResolver propResolver = new PropertyResolver();
        forEachFiles(original(), f -> {
            if(f.getName().endsWith(".java")){
                CompilationUnitTree tree = ProjectUnit.parseFile(f, compiler);
                try{
                    Tree.visit(tree, t -> {
                        if(t instanceof ClassTree c){
                            switch(c.declType()){
                                case CLASS -> {
                                    int fieldCount = 0;
                                    int methodCount = 0;
                                    for(Tree member : c.members()){
                                        if(member instanceof VariableTree v && !v.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                                            fieldCount++;
                                            classData.fieldTypes().count(typeResolver.getFullyQualifiedName(v.actualType().toQualifiedTypeName(), tree.imports()));
                                        }else if(member instanceof MethodTree m && !m.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                                            if(propResolver.isNonImplicitMethod(m, c)){
                                                methodCount++;
                                            }
                                        }
                                    }
                                    classData.numOfFields().count(fieldCount);
                                    classData.numOfMethods().count(methodCount);
                                    for(TypeTree implementation : c.implementsClause()){
                                        classData.interfaces().count(typeResolver.getFullyQualifiedName(implementation.toQualifiedTypeName(), tree.imports()));
                                    }
                                    classData.numOfInterfaces().count(c.implementsClause().size());
                                }
                                case RECORD -> {
                                    if(c.recordComponents() != null){
                                        for(VariableTree v : c.recordComponents()){
                                            recordData.fieldTypes().count(typeResolver.getFullyQualifiedName(v.actualType().toQualifiedTypeName(), tree.imports()));
                                        }
                                    }
                                    recordData.numOfFields().count(c.recordComponents().size());
                                    for(TypeTree implementation : c.implementsClause()){
                                        recordData.interfaces().count(typeResolver.getFullyQualifiedName(implementation.toQualifiedTypeName(), tree.imports()));
                                    }
                                    recordData.numOfInterfaces().count(c.implementsClause().size());
                                    int methodCount = 0;
                                    for(Tree member : c.members()){
                                        if(member instanceof MethodTree m && !m.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                                            if(propResolver.isNonImplicitMethod(m, c)){
                                                methodCount++;
                                            }
                                        }
                                    }
                                    recordData.numOfMethods().count(methodCount);
                                }
                                default -> {}
                            }
                        }
                    });
                }catch(CompileException e){
                    System.out.println(e);
                }
            }
        });
    }

    public String mineRecordHistory() throws IOException{
        Date since = new Date(2020, 4, 1);
        String buf = "";
        int numSave = 0;
        CommitInfo commitSave = null;
        for(Date date = HEAD_COMMIT_STAMP.clone(); date.compareTo(since) >= 0; date = date.previousMonth()){
            CommitInfo commit = spec.commitTrace().revert(date);
            if(commit == null){
                numSave = 0;
            }else if(!commit.equals(commitSave)){
                System.out.println("reverted to %s (%s)".formatted(commit.id().toString(16), commit.date()));
                SimpleCounter counter = new SimpleCounter();
                commitSave = commit;
                checkout(commit.id());
                new ProjectUnit(copied(), t -> {
                    if(t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                        counter.countUp();
                    }
                });
                numSave = counter.getCount();
            }
            buf = "," + numSave + buf;
        }
        return getProjectName() + buf;
    }

    public void query(CEConsumer<Tree> query){
        new ProjectUnit(original(), query);
    }

    public void queryAll(Collection<CEConsumer<Tree>> queries){
        new ProjectUnit(original(), t -> {
            for(var query : queries){
                query.accept(t);
            }
        });
    }

    public boolean refreshCopies() throws IOException{
        System.out.println("refreshing %s ...".formatted(copied()));
        UnaryOperator<String> repository = "%s/%s/%s/".formatted(project().getName(), "%s", getProjectName())::formatted;
        return run("cp -fav %s %s\n".formatted(repository.apply("original"), repository.apply("copied")), PROJECTS_PARENT);
    }

    public String getFetchURL() throws IOException{
        try(var reader = getStreamReader("git remote show origin", original())){
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
        return run("git checkout %s -f".formatted(id.toString(16)), copied());
    }

    public String collectDifferenceData() throws IOException{
        System.out.println("collecting commit info of %s...".formatted(getProjectName()));
        JavaCompiler compiler = new JavaCompiler();
        File workDir = new File("out/rq2-1/diffs/" + getProjectName());

        deleteAll(workDir);
        workDir.mkdirs();

        //a file which summerizes the result of single repository
        File repStat = new File(workDir, "repository-info.csv");
        CommitInfo[] commitTrace = spec.commitTrace().commits();

        try(FileWriter repStatOut = new FileWriter(repStat)){
            File backup = new File("out/rq2-1/backup");
            int[] sumOfRepository = new int[9];
            String buf;
            repStatOut.write(getProjectName() + "\n");
            for(int j = 1; j < commitTrace.length; j++){
                backup.mkdir();
                System.out.println(commitTrace[j-1].date());
                List<File> editedFiles = getChangedFiles();
                editedFiles.removeIf(f -> !f.getName().endsWith(".java"));

                int id = 0;
                for(File after : editedFiles){
                    copyFiles(after, new File(backup, "diff%d_after.java".formatted(++id)));
                }

                checkout(commitTrace[j].id());

                Map<File, CommitDifferenceInfo> diffs = new HashMap<>();
                id = 0;
                for(File before : editedFiles){
                    File after = new File(backup, "diff%d_after.java".formatted(++id));
                    CommitDifferenceInfo info = new CommitDifferenceInfo(
                        commitTrace[j-1], new TypeSet(before, compiler), new TypeSet(after, compiler));
                    for(int k = 0; k < info.data().length; k++){
                        if(info.data()[k] > 0){
                            diffs.put(before, info);
                            break;
                        }
                    }
                }

                if(!diffs.isEmpty()){
                    File commitDir = new File(workDir, commitTrace[j-1].date().toStringForNamingUsage());
                    commitDir.mkdir();

                    File commitInfo = new File(commitDir, "commit-info.txt");
                    try(FileWriter commitInfoWriter = new FileWriter(commitInfo)){
                        int[] sumOfCommit = new int[9];
                        commitInfoWriter.write("commit %s (%s)\n".formatted(commitTrace[j-1].id().toString(16), commitTrace[j-1].date() ));
                        id = 0;

                        for(var f : editedFiles){
                            File diffDir = new File(commitDir, "file%d".formatted(++id));
                            File after = new File(backup, "diff%d_after.java".formatted(id));
                            diffDir.mkdir();
                            copyFiles(f, new File(diffDir, "before.java"));
                            copyFiles(after, new File(diffDir, "after.java"));

                            if(diffs.keySet().contains(f)){
                                CommitDifferenceInfo diff = diffs.get(f);
                                int[] data = diff.data();
                                buf = "";
                                for(int k = 0; k < data.length; k++){
                                    buf += data[k] + ((k == data.length - 1)? "\n" : ",");
                                    sumOfCommit[k] += data[k];
                                }
                                commitInfoWriter.write("%d,%s,%s".formatted(id, diff.before().path, buf));
                            }
                        }
                        buf = "";
                        for(int k = 0; k < sumOfCommit.length; k++){
                            buf += sumOfCommit[k] + ((k == sumOfCommit.length - 1)? "\n" : ",");
                            sumOfRepository[k] += sumOfCommit[k];
                        }
                        commitInfoWriter.write("total,"+buf);
                        repStatOut.write("%s,%s".formatted(commitTrace[j-1].date(), buf));
                    }
                }
                deleteAll(backup);
            }
            buf = "";
            for(int k = 0; k < sumOfRepository.length; k++){
                buf += sumOfRepository[k] + ((k == sumOfRepository.length - 1)? "" : ",");
            }
            repStatOut.write("total,"+buf);
            return "%s,%s".formatted(getProjectName(), buf);
        }
    }

    public List<File> getChangedFiles() throws IOException{
        List<File> changedFiles = new ArrayList<>();
        try(var reader = getStreamReader("git diff HEAD~ --name-only", copied())){
            String buf;
            while((buf = reader.readLine()) != null){
                changedFiles.add(new File("%s/%s".formatted(copied(), buf)));
            }
        }
        return changedFiles;
    }

    public String collectClassData() throws IOException{
        final PropertyResolver propResolver = new PropertyResolver();
        int[] data = new int[9];
        new ProjectUnit(original(), t -> {
            if(t instanceof ClassTree c && c.declType() == DeclarationType.CLASS){
                int result = propResolver.getClassData(c);
                data[0]++;
                for(int i = 1; i < 8; i++){
                    data[i] += (result>>(i-1))&1;
                }
                if((result & 0b0011111) == 0){
                    data[8]++;
                }
            }
        });
        String buf = getProjectName() + ",";
        for(int i = 0; i < data.length; i++){
            buf += data[i] + ((i == data.length - 1)? "" : ",");
        }
        return buf;
    }

    public String collectClassesAndExpressions(){
        int[] data = new int[9];
        new ProjectUnit(original(), t -> {
            if(t instanceof ClassTree c){
                switch(c.declType()){
                    case CLASS -> data[0]++;
                    case ENUM -> data[1]++;
                    case INTERFACE -> data[2]++;
                    case ANNOTATION_INTERFACE -> data[3]++;
                    case RECORD -> data[4]++;
                    default -> {}
                }
                data[5]++;
            }else if(t instanceof AssignmentTree as){
                data[6]++;
            }else if(t instanceof VariableTree v){
                do{
                    if(v.initializer() != null){
                        data[6]++;
                    }
                }while((v = v.follows()) != null);
            }else if(t instanceof MethodInvocationTree mi){
                data[7] += mi.arguments().size();
            }else if(t instanceof NewClassTree nc){
                data[7] += nc.arguments().size();
            }
        });
        data[8] = data[6] + data[7]; 
        String buf = getProjectName() + ",";
        for(int i = 0; i < data.length; i++){
            buf += data[i] + ((i == data.length - 1)? "" : ",");
        }
        return buf;
    }

    public String collectAccessorsInfo(){
        final PropertyResolver propResolver = new PropertyResolver();
        int[] data = new int[3];
        new ProjectUnit(original(), t -> {
            if(t instanceof ClassTree c){
                c.members().forEach(member -> {
                    if(member instanceof MethodTree m){
                        if(propResolver.isEffectivelyGetter(m, c)){
                            data[0]++;
                        }
                        if(propResolver.isGetPrefixedMethod(m) && propResolver.isEffectivelyGetter(m, c)){
                            data[1]++;
                        }
                        if(propResolver.isRecordFormatGetter(m, c) && propResolver.isEffectivelyGetter(m, c)){
                            data[2]++;
                        }
                    }
                });
            }
        });
        String buf = getProjectName() + ",";
        for(int i = 0; i < data.length; i++){
            buf += data[i] + ((i == data.length - 1)? "" : ",");
        }
        return buf;
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
        return spec.commitTrace().commits();
    }

    public File project(){
        return spec.project();
    }

    public File original(){
        return spec.original();
    }

    public File copied(){
        return spec.copied();
    }

    public String getProjectName(){
        return spec.original().getName();
    }

    public ProjectSpec getSpec(){
        return spec;
    }

    public boolean isCompatibleWithJava16(){
        return spec.metrics().version() != JavaVersion.BEFORE_JAVA16;
    }

    public boolean hasRecords(){
        return spec.metrics().version() == JavaVersion.HAS_RECORDS;
    }

}
