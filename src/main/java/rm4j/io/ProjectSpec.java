package rm4j.io;

import java.io.File;
import java.io.Serializable;

public record ProjectSpec(File project, File original, File copied, CommitTrace commitTrace, Metrics metrics) implements Serializable, CSVTuple{

    private static final long serialVersionUID = 0x8A2D07CAD2B99F51L;

    public String getProjectName(){
        return original.getName();
    }

    @Override
    public String toCSVLine() {
        return getProjectName() + "," + metrics.toString();
    }

}