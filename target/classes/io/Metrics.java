package rm4j.io;

public record Metrics(String repName, double age, int commits, int authors, int javaFiles, JavaVersion version) implements CSVTuple{

    enum JavaVersion{
        BEFORE_JAVA16,
        AFTER_JAVA16,
        HAS_RECORDS;
    }

    @Override
    public String toCSVLine(){
        return "%s, %f, %d, %d, %d, %s".formatted(repName, age, commits, authors, javaFiles, version.name());
    }
}
