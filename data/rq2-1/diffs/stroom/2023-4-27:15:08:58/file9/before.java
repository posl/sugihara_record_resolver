package stroom.data.zip;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

class TestStroomZipFile_RealExample {

    @Test
    void testRealZip1() throws IOException {
        final Path sourceFile = Paths.get("./src/test/resources/stroom/data/zip/BlankZip.zip");
        StroomZipFile stroomZipFile = new StroomZipFile(sourceFile);

        ArrayList<String> list = new ArrayList<>(stroomZipFile.getBaseNames());
        Collections.sort(list);

        stroomZipFile.close();
    }
}
