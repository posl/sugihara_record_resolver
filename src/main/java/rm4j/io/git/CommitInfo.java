package rm4j.io.git;

import java.io.Serializable;
import java.math.BigInteger;

public record CommitInfo(BigInteger id, String author, Date date) implements Serializable{

    private static final long serialVersionUID = 0x90150B523061D592L;

    @Override
    public String toString(){
        return "commit %s\n".formatted(id.toString(16))+
                "Author: %s\n".formatted(author)+
                "Date: %s\n".formatted(date.toString());
    }

    @Override
    public int hashCode(){
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if(o != null && o instanceof CommitInfo c){
            return id.equals(c.id);
        }
        return false;
    }
}
