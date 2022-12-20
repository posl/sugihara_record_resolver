package rm4j.io.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.Tree;
import rm4j.util.functions.CEConsumer;

public class RepositoryManager implements Serializable{

    private static final long serialVersionUID = 0xF4D9F068C644D8DCL;
    public static final Date SINCE = new Date(2020, 3, 14);

    private final File repository;

    private final CommitInfo[] commitTrace;

    public RepositoryManager(File repository) throws IOException{
        this.repository = repository;
        commitTrace = createCommitTrace();
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
