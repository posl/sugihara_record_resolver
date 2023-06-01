package rm4j.io.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.Tree;
import rm4j.util.functions.CEConsumer;

public class RepositoryManager implements Serializable{

    private static final long serialVersionUID = 0xF4D9F068C644D8DCL;
    public static final Date SINCE = new Date(2020, 3, 1);

    private final File project;

    private final CommitInfo[] commitTrace;

    public RepositoryManager(File project) throws IOException{
        this.project = project;
        commitTrace = createCommitTrace();
    }

    public String getFetchURL() throws IOException{
        try(var reader = getStreamReader("git remote show origin")){
            String buf;
            while((buf = reader.readLine()) != null && !buf.startsWith("    ")){
                if(buf.startsWith("  Fetch URL: ")){
                    return buf.split("  Fetch URL: ")[1];
                }
            }
        }
        return "";
    }

    private CommitInfo[] createCommitTrace() throws IOException{
        List<CommitInfo> commitTrace = new LinkedList<>();
        String defaultBranchName = null;
        try(var reader = getStreamReader("git remote show origin")){
            String buf;
            while((buf = reader.readLine()) != null && !buf.startsWith("    ")){
                if(buf.startsWith("  HEAD branch: ")){
                    defaultBranchName = buf.split("  HEAD branch: ")[1];
                }
            }
        }
        run("git checkout %s -f".formatted(defaultBranchName));
        CommitInfo top = getCommitInfo(getStreamReader("git show HEAD"));
        int i = 0;
        while(top != null){
            commitTrace.add(top);
            i++;
            System.out.println(top.date());
            if(top.date().compareTo(SINCE) < 0){
                break;
            }else{
                top = getCommitInfo(getStreamReader("git show HEAD~%d".formatted(i)));
            }
        }
        CommitInfo[] commitTraceArray = new CommitInfo[i];
        i = 0;
        for(CommitInfo commit : commitTrace){
            commitTraceArray[i++] = commit;
        }
        return commitTraceArray;
    }

    private CommitInfo getCommitInfo(final BufferedReader reader) throws IOException{
        try(reader){
            final BigInteger id;
            final String author;
            final Date date;
            String buf;
            if((buf = reader.readLine()) == null){
                return null;
            }
            id = new BigInteger(buf.split("commit | ")[1], 16);
            if((buf = reader.readLine()) != null){
                if(buf.startsWith("Merge: ")){
                    buf = reader.readLine();
                    if(buf == null){
                        throw new IOException("Invalid commit information.");
                    }
                }
                author = buf.split("Author: ")[1];
                if((buf = reader.readLine()) != null){
                    date = parseDate(buf.split("Date:   ")[1]);
                    return new CommitInfo(id, author, date);
                }
            }
            throw new IOException("Invalid commit information.");
        }catch(IndexOutOfBoundsException e){
            throw new IOException("Invalid commit information.");
        }
    }
    
    private boolean run(String cmd) throws IOException{
        Process p = Runtime.getRuntime().exec(cmd, null, repository);
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

    private InputStream getStream(String command) throws IOException{
        return Runtime.getRuntime().exec(command, null, repository).getInputStream();
    }

    private BufferedReader getStreamReader(String command) throws IOException{
        return new BufferedReader(new InputStreamReader(getStream(command)));
    }

    public void fetch() throws IOException{
        if (run("git fetch")){
            System.out.println("fetched %s".formatted(repository.getName()));
        }
    }

    public void createProjectUnit(CEConsumer<Tree> query){
        new ProjectUnit(repository, query);
    }

    public boolean checkout(BigInteger id) throws IOException{
        return run("git checkout %s -f".formatted(id.toString(16)));
    }

    public List<File> getChangedFiles() throws IOException{
        List<File> changedFiles = new ArrayList<>();
        try(var reader = getStreamReader("git diff HEAD~ --name-only")){
            String buf;
            while((buf = reader.readLine()) != null){
                changedFiles.add(new File("%s/%s".formatted(repository, buf)));
            }
        }
        return changedFiles;
    }

    public String collectDifferenceData(int repositoryId, File workingDir) throws IOException{
        System.out.println("%d: %s".formatted(repositoryId, repository.getName()));
        JavaCompiler compiler = new JavaCompiler();
        File outDir = new File("work/commitDifference/%s".formatted(repository.getName()));
        resetDir(outDir);

        //a file which summerizes the result of single repository
        File repositoryInfo = new File(outDir, "repository-info.txt");

        //revert the version of the repository to HEAD
        while(!checkout(commitTrace[0].id()));

        try(FileWriter repositoryInfoWriter = new FileWriter(repositoryInfo)){
            int[] sumOfRepository = new int[9];
            String buf;
            repositoryInfoWriter.write(repository.getName() + "\n");
            for(int j = 1; j < commitTrace.length; j++){
                System.out.println(commitTrace[j-1].date());
                resetDir(workingDir);
                List<File> editedFiles = getChangedFiles();
                editedFiles.removeIf(f -> !f.getName().endsWith(".java"));

                int id = 0;
                for(File after : editedFiles){
                    copyFile(after, new File(workingDir, "diff%d_after.java".formatted(++id)));
                }

                checkout(commitTrace[j].id());

                List<CommitDifferenceInfo> diffs = new ArrayList<>();
                id = 0;
                for(File before : editedFiles){
                    File after = new File(workingDir, "diff%d_after.java".formatted(++id));
                    CommitDifferenceInfo info = new CommitDifferenceInfo(
                        commitTrace[j-1], new TypeSet(before, compiler), new TypeSet(after, compiler));
                    for(int k = 0; k < info.data().length; k++){
                        if(info.data()[k] > 0){
                            diffs.add(info);
                            break;
                        }
                    }
                }

                if(!diffs.isEmpty()){
                    File commitDir = new File(outDir, commitTrace[j-1].date().toStringForNamingUsage());
                    commitDir.mkdir();

                    File commitInfo = new File(commitDir, "commit-info.txt");
                    try(FileWriter commitInfoWriter = new FileWriter(commitInfo)){
                        int[] sumOfCommit = new int[9];
                        commitInfoWriter.write("commit %s (%s)\n".formatted(commitTrace[j-1].id().toString(16), commitTrace[j-1].date() ));
                        int fileId = 0;
                        for(var diff : diffs){
                            File diffDir = new File(commitDir, "file%d".formatted(++fileId));
                            diffDir.mkdir();
                            copyFile(diff.before().path, new File(diffDir, "before.java"));
                            copyFile(diff.after().path, new File(diffDir, "after.java"));
                            int[] data = diff.data();
                            buf = "";
                            for(int k = 0; k < data.length; k++){
                                buf += data[k] + ((k == data.length - 1)? "\n" : ", ");
                                sumOfCommit[k] += data[k];
                            }
                            commitInfoWriter.write("%d: %s, %s".formatted(fileId, diff.before().path, buf));
                        }
                        buf = "";
                        for(int k = 0; k < sumOfCommit.length; k++){
                            buf += sumOfCommit[k] + ((k == sumOfCommit.length - 1)? "\n" : ", ");
                            sumOfRepository[k] += sumOfCommit[k];
                        }
                        commitInfoWriter.write("sum: "+buf);
                        repositoryInfoWriter.write("%s: %s".formatted(commitTrace[j-1].date(), buf));
                    }
                }
            }
            buf = "";
            for(int k = 0; k < sumOfRepository.length; k++){
                buf += sumOfRepository[k] + ((k == sumOfRepository.length - 1)? "\n" : ", ");
            }
            repositoryInfoWriter.write("sum: "+buf);
            return "%d: %s, %s".formatted(repositoryId, repository.getName(), buf);
        }
    }

    private static boolean copyFile(File original, File target){
        if(!original.exists()){
            return false;
        }
        try(FileReader reader = new FileReader(original);
            FileWriter writer = new FileWriter(target)){
            reader.transferTo(writer);
            return true;
        }catch(IOException e){
            System.out.println(e);
            return false;
        }
    }

    private static boolean resetDir(File dir){
        if(dir.exists()){
            deleteAll(dir);
        }
        return dir.mkdir();
    }

    private static boolean deleteAll(File file){
        if(file.isDirectory()){
            boolean status = true;
            for(File f : file.listFiles()){
                status &= deleteAll(f);
            }
            return status && file.delete();
        }else{
            return file.delete();
        }
    }

    private static Date parseDate(String s){
        String contents[] = s.split(" |:");
        return new Date(Integer.parseInt(contents[6]),
                Date.convertMonth(contents[1]),
                Integer.parseInt(contents[2]),
                Integer.parseInt(contents[3]),
                Integer.parseInt(contents[4]),
                Integer.parseInt(contents[5]));
    }

    public File repository(){
        return repository;
    }

    public CommitInfo[] commitTrace(){
        return commitTrace;
    }

}
