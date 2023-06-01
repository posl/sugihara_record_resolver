package rm4j.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rm4j.compiler.tree.*;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;

public class ClassTotalizer{

    private int[] classData = new int[8];

    private Ranker<String> classFieldTypes = new Ranker<>(65536);
    private Ranker<Integer> numOfFields = new Ranker<>(64);
    private Ranker<String> classFinalFieldTypes = new Ranker<>(65536);
    private Ranker<Integer> numOfFinalFields = new Ranker<>(64);
    private Ranker<String> implementedInterfaces = new Ranker<>(4096);
    private Ranker<Integer> numOfImplementedInterfaces = new Ranker<>(16);
    private Ranker<Integer> numOfTypeParameters = new Ranker<>(16);
    private Ranker<String> wordsForClassName = new Ranker<>(65536);
    private Ranker<String> classAttribute = new Ranker<>(4096);

    public void collectInformation(ClassTree c){
        if(c.declType() == DeclarationType.CLASS){
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
                wordsForClassName.count(word);
            }
            if(!wordsOfName.isEmpty()){
                classAttribute.count(wordsOfName.get(wordsOfName.size()-1));
            }

            int num = 0;
            int numOfFinal = 0;
            for(Tree m : c.members()){
                if(m instanceof VariableTree v){
                    VariableTree field = v;
                    do{
                        if(!field.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                            num++;
                            classFieldTypes.count(field.actualType().toQualifiedTypeName());
                            if(field.modifiers().getModifiers().contains(ModifierKeyword.FINAL)){
                                numOfFinal++;
                                classFinalFieldTypes.count(field.actualType().toQualifiedTypeName());
                            }
                        }
                    }while((field = field.follows()) != null);
                }
            }
            if(num > 8000){
                System.out.println("%s, %d".formatted(c.name().toSource(""), num));
            }
            numOfFields.count(num);
            numOfFinalFields.count(numOfFinal);
            numOfImplementedInterfaces.count(c.implementsClause().size());
            numOfTypeParameters.count(c.typeParameters().size());
            for(var implemention : c.implementsClause()){
                implementedInterfaces.count(implemention.toQualifiedTypeName());
            }
        }
    }

    public void extractClassInformation(ClassTree c){
        if(c.declType() != DeclarationType.CLASS){
            return;
        }
        classData[0]++;
        if(!c.modifiers().getModifiers().contains(ModifierKeyword.ABSTRACT)
            && !c.modifiers().getModifiers().contains(ModifierKeyword.SEALED)
            && !c.modifiers().getModifiers().contains(ModifierKeyword.NON_SEALED)
            && c.extendsClause() == null){
            for(Tree member : c.members()){
                if(member instanceof BlockTree b && !b.isStatic()){
                    return;
                }
            }
            classData[1]++;
            List<VariableTree> finalFields = new ArrayList<>();
            for(Tree member : c.members()){
                if(member instanceof VariableTree v){
                    VariableTree field = v;
                    do{
                        if(!field.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                            if(!field.modifiers().getModifiers().contains(ModifierKeyword.FINAL)){
                                return;
                            }
                            finalFields.add(field);
                        }
                    }while((field = field.follows()) != null);
                }
            }
            classData[2]++;
            if(finalFields.isEmpty()){
                return;
            }
            classData[3]++;
            boolean hasCorrespondingMethod = false;
            for(Tree member : c.members()){
                if(member instanceof MethodTree m){
                    if(isCanonicalConstructor(m, finalFields)){
                        classData[4]++;
                        hasCorrespondingMethod = true;
                        break;
                    }
                }
            }
            for(Tree member : c.members()){
                if(member instanceof MethodTree m){
                    if(RecordComparator.isEqualsMethod(m)
                        || RecordComparator.isHashCodeMethod(m)
                        || RecordComparator.isToStringMethod(m)
                        || RecordComparator.isGetterMethod(c, m)){
                        classData[5]++;
                        hasCorrespondingMethod = true;
                        break;
                    }
                }
            }
            if(!hasCorrespondingMethod){
                return;
            }
            classData[6]++;
            if(!c.modifiers().getModifiers().contains(ModifierKeyword.FINAL)){
                return;
            }
            classData[7]++;
        }
    }

    public void writeData() throws IOException{
        // File directory = new File("work/class2");
        // String s = "";
        // directory.mkdir();
        // System.out.println("total data:");
        // for(int i = 0; i < classData.length; i++){
        //     s += classData[i] + ((i ==  classData.length - 1)? "" : ", ");
        // }
        // System.out.println(s);
        writeDataToSingleFile(classFieldTypes, "classFiledTypes.csv");
        writeDataToSingleFile(numOfFields, "numOfClassFields.csv");
        writeDataToSingleFile(classFinalFieldTypes, "classFinalFieldTypes.csv");
        writeDataToSingleFile(numOfFinalFields, "numOfFinalFields.csv");
        writeDataToSingleFile(implementedInterfaces, "classInterfaces.csv");
        writeDataToSingleFile(numOfImplementedInterfaces, "numOfClassInterfaces.csv");
        writeDataToSingleFile(wordsForClassName, "wordsForClassName.csv");
        writeDataToSingleFile(classAttribute, "classAttribute.csv");
        writeDataToSingleFile(numOfTypeParameters, "numOfClassTypeParameters.csv");
    }

    private static <T> void writeDataToSingleFile(Ranker<T> ranker, String fileName) throws IOException{
        File out = new File("work/class3/"+fileName);
        try(FileWriter writer = new FileWriter(out)){
            int n = 0;
            for(var column : ranker.getRanking()){
                writer.write("%d: %s: %d\n".formatted(++n, column.t(), column.count()));
            }
        }
    }

    private static boolean isCanonicalConstructor(MethodTree m, List<VariableTree> finalFields){
        if(RecordComparator.isConstructor(m) && m.typeParameters().isEmpty() && m.exceptions().isEmpty()){
            if(m.parameters().size() != finalFields.size()){
                return false;
            }
            OUTER : for(var parameter : m.parameters()){
                String parameterType = parameter.actualType().toQualifiedTypeName();
                if(parameterType.endsWith("...")){
                    parameterType = parameterType.substring(0, parameterType.length()-3) + "[]";
                }
                for(int i = 0; i < finalFields.size(); i++){
                    if(finalFields.get(i).actualType().toQualifiedTypeName().equals(parameterType)){
                        finalFields.remove(i);
                        continue OUTER;
                    }
                }
                return false;
            }
            return finalFields.isEmpty();
        }
        return false;
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
