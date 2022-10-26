package rm4j.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import rm4j.compiler.tree.AnnotatedTypeTree;
import rm4j.compiler.tree.ArrayTypeTree;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.ParameterizedTypeTree;
import rm4j.compiler.tree.PrimitiveTypeTree;
import rm4j.compiler.tree.TypeTree;
import rm4j.compiler.tree.VariableArityTypeTree;
import rm4j.compiler.tree.VariableTree;
import rm4j.compiler.tree.Tree.DeclarationType;

import static rm4j.io.git.DatasetManager.REPOSITORIES;

public class RecordTotalizer{

    private int numOfRecords = 0;
    private Map<TypeInfo, Integer> componentTypeData = new ConcurrentHashMap<>(64);
    private Map<Integer, Integer> componentSizeData = new ConcurrentHashMap<>(10);
    private Map<Integer, Integer> interfaceData = new ConcurrentHashMap<>(10);
    private Map<Integer, Integer> genericsData = new ConcurrentHashMap<>(10);
    private static final Set<String> SUBJECT_TYPES = new HashSet<>(Arrays.asList(
        "byte", "short", "int", "long", "float", "double", "char", "boolean", "String", "Object", "Byte", "Short", "Integer", "Long", "Float", "Double", "Character", "Boolean", "File", "List", "Map", "Set"
    ));

    private Map<File, Integer> repositoryData = new ConcurrentHashMap<>(50);
    private Map<File, Integer> repositoryData2 = new ConcurrentHashMap<>(50);
    private Map<File, Integer> repositoryData3 = new ConcurrentHashMap<>(50);
    private Map<File, File> fileCorrespondence = new ConcurrentHashMap<>(300);
    private static final File DEBUG_SOURCE_DIR = new File("/output");
    private static volatile int debugFileNumber = 0;
    //private static int debugDirectoryNumber = 0;

    public void totalize(ClassTree t, File f){
        
        if(t.declType() == DeclarationType.RECORD && t.recordComponents() != null && t.typeParameters() != null && t.implementsClause() != null){
            synchronized(this){
                numOfRecords++;
            }
            getSource(f);
            for(VariableTree v : t.recordComponents()){
                registerTypeInfo(getRootType(v.actualType(), 0));
            }
            registerNumber(t.recordComponents().size(), componentSizeData);
            registerNumber(t.typeParameters().size(), genericsData);
            registerNumber(t.implementsClause().size(), interfaceData);
        }
        
    }

    TypeInfo getRootType(TypeTree t, int dimension){
        if(t instanceof AnnotatedTypeTree ann){
            return getRootType(ann.type(), dimension);
        }else if(t instanceof ArrayTypeTree arr){
            return getRootType(arr.elementType(), dimension+1);
        }else if(t instanceof VariableArityTypeTree ari){
            return getRootType(ari.type(), dimension+1);
        }else if(t instanceof ParameterizedTypeTree par){
            return getRootType((TypeTree)par.type(), dimension);
        }else if(t instanceof PrimitiveTypeTree prim){
            return new TypeInfo(prim.primitiveType().name().toLowerCase(), dimension);
        }
        return new TypeInfo(t.toString(), dimension);
    }

    void registerTypeInfo(TypeInfo info){
        if(!SUBJECT_TYPES.contains(info.typeName())){
            info = TypeInfo.REFERENCE_TYPE;
        }
        registerNumber(info, componentTypeData);
    }

    synchronized <K> void registerNumber(K key, Map<K, Integer> map){
        if(map.get(key) == null){
            map.put(key, 1);
        }else{
            map.put(key, map.get(key)+1);
        }
    }

    public void print(){
        System.out.format("number of total records : %d\n", numOfRecords);

        System.out.println("\nrecord usecase repositories:");
        printMap(repositoryData);

        System.out.println("\nnum of type parameters:");
        printMap(genericsData);
        System.out.println("\ncomponent size data:");
        printMap(componentSizeData);
        System.out.println("\ncomponent type data:");
        printMap(componentTypeData);
        System.out.println("\nnum of implemented interfaces:");
        printMap(interfaceData);

        System.out.println("\nswitch expression usecase repositories:");
        printMap(repositoryData2);
    }

    <K> void printMap(Map<K, Integer> map){
        for(var e : map.entrySet()){
            System.out.format("%s --- %d\n", e.getKey().toString(), e.getValue());
        }
    }

    private void getSource(File f){

        if(fileCorrespondence.get(f) != null){
            return;
        }
        File dir = getRepository(f);

        synchronized(this){
            File created = new File(dir.toString() + "/USECASE_%s.java".formatted(++debugFileNumber));
            fileCorrespondence.put(f, created);
            try{
                created.createNewFile();
                try(FileInputStream reader = new FileInputStream(f);
                    FileOutputStream writer = new FileOutputStream(created)){
                    FileChannel in = reader.getChannel();
                    in.transferTo(0, in.size(), writer.getChannel());
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println(e);
            }
        }

    }

    private File getRepository(File f){
        File repository = f;
        do{
            repository = repository.getParentFile();
        }while(!repository.getParentFile().equals(REPOSITORIES));
        String directoryName = repository.getName();
        File dir = new File(DEBUG_SOURCE_DIR + "/" +directoryName);
        if(repositoryData.get(dir) == null){
            repositoryData.put(dir, 1);
            dir.mkdir();
        }else{
            repositoryData.put(dir, repositoryData.get(dir) + 1);
        }
        return dir;
    }

    record TypeInfo(String typeName, int dimension){
        static final TypeInfo REFERENCE_TYPE = new TypeInfo("#REF", 0);

        @Override
        public String toString(){
            String dims = "";
            for(int i = 0; i < dimension; i++){
                dims += "[]";
            }
            return typeName + dims;
        }
    }

    public void countRecordPattern(File f){
        if(fileCorrespondence.get(f) != null){
            return;
        }
        File repository = f;
        do{
            repository = repository.getParentFile();
        }while(!repository.getParentFile().equals(REPOSITORIES));
        String directoryName = repository.getName();
        File dir = new File(DEBUG_SOURCE_DIR + "/" +directoryName);
        if(repositoryData3.get(dir) == null){
            repositoryData3.put(dir, 1);
            dir.mkdir();
        }else{
            repositoryData3.put(dir, repositoryData3.get(dir) + 1);
        }
    }
}
