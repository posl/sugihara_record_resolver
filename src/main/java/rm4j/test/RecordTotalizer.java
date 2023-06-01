package rm4j.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rm4j.compiler.tree.*;
import rm4j.compiler.tree.Tree.DeclarationType;

public class RecordTotalizer{

    private Ranker<String> recordHeaderTypes = new Ranker<>(2048);
    private Ranker<Integer> sizeOfHeader = new Ranker<>(64);
    private Ranker<String> implementedInterfaces = new Ranker<>(2048);
    private Ranker<Integer> numOfImplementedInterfaces = new Ranker<>(16);
    private Ranker<Integer> numOfTypeParameters = new Ranker<>(16);
    private Ranker<String> wordsForRecordName = new Ranker<>(4096);
    private Ranker<String> recordAttribute = new Ranker<>(2048);

    public void collectInformation(ClassTree c){
        if(c.declType() == DeclarationType.RECORD && c.recordComponents() != null){
            char[] nameCharacters = c.name().toSource("").toCharArray();
            String s = "";
            List<String> wordsOfName = new ArrayList<>();
            for(int i = 0; i < nameCharacters.length; i++){
                char ch = nameCharacters[i];
                if(Character.isUpperCase(ch)){
                    if(!s.equals("")){
                        wordsOfName.add(s);
                        s = "";
                    }
                    s += Character.toLowerCase(ch);
                }else if(Character.isLowerCase(ch)){
                    s += ch;
                }else{
                    if(!s.equals("")){
                        wordsOfName.add(s);
                        s = "";
                    }
                }
            }
            if(!s.equals("")){
                wordsOfName.add(s);
            }
            for(String word : wordsOfName){
                wordsForRecordName.count(word);
            }
            if(!wordsOfName.isEmpty()){
                recordAttribute.count(wordsOfName.get(wordsOfName.size()-1));
            }
            sizeOfHeader.count(c.recordComponents().size());
            numOfImplementedInterfaces.count(c.implementsClause().size());
            numOfTypeParameters.count(c.typeParameters().size());
            for(var recordComponent : c.recordComponents()){
                recordHeaderTypes.count(recordComponent.declaredType().toQualifiedTypeName());
            }
            for(var implemention : c.implementsClause()){
                implementedInterfaces.count(implemention.toQualifiedTypeName());
            }
        }else if(c.declType() == DeclarationType.RECORD && c.recordComponents() == null){
            sizeOfHeader.count(null);
            System.out.println("Invalid record");
        }
    }

    public void writeData() throws IOException{
        writeDataToSingleFile(recordHeaderTypes, "recordHeaderTypes.csv");
        writeDataToSingleFile(sizeOfHeader, "sizeOfHeader.csv");
        writeDataToSingleFile(implementedInterfaces, "interfaces.csv");
        writeDataToSingleFile(numOfImplementedInterfaces, "numOfInterfaces.csv");
        writeDataToSingleFile(wordsForRecordName, "wordsForRecordName.csv");
        writeDataToSingleFile(recordAttribute, "recordAttribute.csv");
        writeDataToSingleFile(numOfTypeParameters, "numOfTypeParameters.csv");
    }

    private static <T> void writeDataToSingleFile(Ranker<T> ranker, String fileName) throws IOException{
        File out = new File("work/record2/"+fileName);
        try(FileWriter writer = new FileWriter(out)){
            int n = 0;
            for(var column : ranker.getRanking()){
                writer.write("%d: %s, %d\n".formatted(++n, column.t(), column.count()));
            }
        }
    }

    class Ranker<T>{

        final Map<T, Integer> data;

        public Ranker(int initialCapacity){
            this.data = new HashMap<>(initialCapacity);
        }

        public void count(T t){
            Integer count;
            if((count = data.get(t)) == null){
                data.put(t, 1);
            }else{
                data.put(t, count+1);
            }
        }

        public List<RankerComponent<T>> getRanking(){
            List<RankerComponent<T>> ranking = new ArrayList<>();
            for(var e : data.entrySet()){
                ranking.add(new RankerComponent<>(e.getKey(), e.getValue()));
            }
            ranking.sort((c1, c2) -> {
                if(c1.count() > c2.count()){
                    return -1;
                }else if(c1.count() < c2.count()){
                    return 1;
                }
                return 0;
            });
            return ranking;
        }
        
    }

    record RankerComponent<T>(T t, int count){};

}
