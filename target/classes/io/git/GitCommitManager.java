package rm4j.io.git;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.Tree;
import rm4j.util.functions.CEConsumer;

public class GitCommitManager{

    public static final String SINCE = "2020/3/14";
    public static final String UNTIL = "2022/10/14";

    private final File repository;
    private final String defaultBranchName;
    private final CommitInfo[] commits;

    public GitCommitManager(File repository) throws IOException{
        this.repository = repository;
        this.defaultBranchName = getDefaultBranchName(Runtime.getRuntime()
            .exec("git remote show origin", null, repository).getInputStream());
        this.commits = extractCommitInfo(
            Runtime.getRuntime()
                .exec("git log --all --since=\"%s\" --until=\"%s\" --date-order %s".formatted(SINCE, UNTIL, defaultBranchName),
                     null, repository)
                .getInputStream());
    }

    public CommitInfo[] commits(){
        return commits;
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

    private String getDefaultBranchName(InputStream in) throws IOException{
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
            for(int i = 0; i < 3; i++){
                reader.readLine();
            }
            return reader.readLine().split("HEAD branch: ")[1];
        }
    }

    private CommitInfo[] extractCommitInfo(InputStream in) throws IOException{
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
            List<CommitInfo> commits = new LinkedList<>();
            String s;
            while ((s = reader.readLine()) != null){
                while (s != null && !s.startsWith("commit ")){
                    s = reader.readLine();
                }
                if (s == null){
                    break;
                }
                commits.add(new CommitInfo(
                        new BigInteger(s.substring(7), 16),
                        cutPrefix(
                                (s = reader.readLine()).startsWith("Merge: ") ? reader.readLine() : s,
                                "Author: "),
                        parse(cutPrefix(reader.readLine(), "Date:   "))));
            }
            commits.sort((c1, c2) -> c1.date().compareTo(c2.date()));
            return commits.toArray(new CommitInfo[0]);
        }
    }

    public void pull() throws IOException{
        if (run("git pull")){
            System.out.println("updated %s".formatted(repository.getName()));
        }
    }

    public void fetch() throws IOException{
        if (run("git fetch")){
            System.out.println("fetched %s".formatted(repository.getName()));
        }
    }

    public void createProjectUnit(CEConsumer<Tree> query){
        new ProjectUnit(repository, query);
    }

    public boolean checkout(Date date) throws IOException{
        if (commits.length == 0){
            return false;
        }
        int i;
        BigInteger id = commits[0].id();
        for (i = 0; i < commits.length; i++){
            if (date.compareTo(commits[i].date()) < 0){
                break;
            }
        }
        i--;
        if (i == -1){
            id = commits[0].id();
            return false;
        }else{
            id = commits[i].id();
        }
        return run("git checkout -f %s".formatted(id.toString(16)));
    }

    public boolean checkout(BigInteger id) throws IOException{
        return run("git checkout -f %s".formatted(id.toString(16)));
    }

    private static String cutPrefix(String s, String prefix) throws IOException{
        if (s == null || !s.startsWith(prefix)){
            System.out.println(s);
            throw new IOException("Invalid commit log.");
        }
        return s.substring(prefix.length());
    }

    private static Date parse(String s){
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

}
