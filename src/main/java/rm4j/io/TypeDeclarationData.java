package rm4j.io;

import java.util.Arrays;
import java.util.List;

public record TypeDeclarationData(
    Ranker<Integer> numOfFields,
    Ranker<String> fieldTypes,
    Ranker<Integer> numOfInterfaces,
    Ranker<String> interfaces,
    Ranker<Integer> numOfMethods
) {
    
    public TypeDeclarationData(){
        this(
            new Ranker<>(64),
            new Ranker<>(1024),
            new Ranker<>(16),
            new Ranker<>(1024),
            new Ranker<>(64)
        );
    }

    public List<Ranker<?>> enumerateRankers(){
        return Arrays.asList(
            numOfFields, fieldTypes, numOfInterfaces, interfaces, numOfMethods);
    }

}
