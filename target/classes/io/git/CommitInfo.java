package rm4j.io.git;

import java.math.BigInteger;

public record CommitInfo(BigInteger id, String author, Date date){

    @Override
    public String toString(){
        return "commit %s\n".formatted(id.toString(16))+
                "Author: %s\n".formatted(author)+
                "Date: %s\n".formatted(date.toString());
    }

}
