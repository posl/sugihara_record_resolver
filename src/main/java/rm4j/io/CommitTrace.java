package rm4j.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

public class CommitTrace implements Serializable{

    private static final long serialVersionUID = 0x8B17E135E8AA6B05L;

    public static final Date SINCE = new Date(2020, 3, 1);
    
    private final CommitInfo trace[];
    private int id;

    public CommitTrace(Terminal terminal) throws IOException{
        this.trace = createTrace(terminal);
        this.id = 0;
    }

    public void setId(Terminal terminal) throws IOException{
        CommitInfo top = getCommitInfo(terminal.run("git show HEAD --"));
        int i;
        for(i = 0; i < trace.length; i++){
            if(trace[i].equals(top)){
                break;
            }
        }
        this.id = i;
    }

    public CommitInfo revert(Date stamp){
        while(stamp.compareTo(trace[id].date()) < 0 && id < trace.length - 1){
            id++;
        }
        if(id == trace.length){
            return null;
        }else{
            return trace[id];
        }
    }

    public CommitInfo[] commits(){
        return trace;
    }

    private CommitInfo[] createTrace(Terminal terminal) throws IOException{
        List<CommitInfo> commitTrace = new LinkedList<>();
        CommitInfo top = getCommitInfo(terminal.run("git show HEAD -- "));
        int i = 0;
        while(top != null){
            commitTrace.add(top);
            i++;
            System.out.println(top.date());
            if(top.date().compareTo(SINCE) < 0){
                break;
            }else{
                top = getCommitInfo(terminal.run("git show HEAD~%d".formatted(i)));
            }
        }
        CommitInfo[] commitTraceArray = new CommitInfo[i];
        i = 0;
        for(CommitInfo commit : commitTrace){
            commitTraceArray[i++] = commit;
        }
        return commitTraceArray;
    }

    public CommitInfo getCommitInfo(final BufferedReader reader) throws IOException{
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
                    date = Date.parse(buf.split("Date:   ")[1]);
                    return new CommitInfo(id, author, date);
                }
            }
            throw new IOException("Invalid commit information.");
        }catch(IndexOutOfBoundsException e){
            throw new IOException("Invalid commit information.");
        }
    }

    @FunctionalInterface
    interface Terminal{
        public BufferedReader run(String cmd) throws IOException;
    }
    
}
