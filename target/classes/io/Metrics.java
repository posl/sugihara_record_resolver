package rm4j.io;

import java.io.Serializable;

public record Metrics(double age, int commits, int authors, int javaFiles, JavaVersion version) implements Serializable{

    public enum JavaVersion{
        BEFORE_JAVA16,
        AFTER_JAVA16,
        HAS_RECORDS;
    }

    @Override
    public String toString(){
        return "%f,%d,%d,%d,%s".formatted(age, commits, authors, javaFiles, version.name());
    }
    
}
