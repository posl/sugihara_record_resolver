package rm4j.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.VariableTree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.io.TypeSet;

public class RecordComparator {
    
    public void compareRecords(){
        int numOfClassToRecordRefactoring = 0;
        int[] decrease = new int[6];
        int[] increase = new int[6];
        int[] increaseCase = new int[6];
        int[] decreaseCase = new int[6];
        int[] existingCase = new int[6];
        File data = new File("work/commitDifference");
        var compiler = new JavaCompiler();
        for(File repository : data.listFiles()){
            for(File commit : repository.listFiles(f -> !f.getName().equals("repository-info.txt"))){
                for(File diff : commit.listFiles(f -> !f.getName().equals("commit-info.txt"))){
                    File beforeFile = new File(diff, "before.java");
                    File afterFile = new File(diff, "after.java");
                    if(beforeFile.exists() && afterFile.exists()){
                        var before = new TypeSet(beforeFile, compiler);
                        var after = new TypeSet(afterFile, compiler);
                        for(var classPath : after.classes.entrySet()){
                            ClassTree recordTree = classPath.getValue();
                            if(recordTree.declType() == DeclarationType.RECORD){
                                ClassTree classTree = before.classes.get(classPath.getKey());
                                if(classTree != null && classTree.declType() == DeclarationType.CLASS){
                                    numOfClassToRecordRefactoring++;
                                    int[] classData = collectMethodData(classTree);
                                    int[] recordData = collectMethodData(recordTree);
                                    for(int i = 0; i < recordData.length; i++){
                                        int sub = recordData[i] - classData[i];
                                        if(recordData[i] > 0 && sub == 0){
                                            existingCase[i]++;
                                        }
                                        if(sub > 0){
                                            increase[i] += sub;
                                            increaseCase[i]++;
                                        }else if(sub < 0){
                                            decrease[i] -= sub;
                                            decreaseCase[i]++;
                                        }
                                    }

                                    System.out.println("%s".formatted(diff));
                                    System.out.print("class : ");
                                    for(int i = 0; i < recordData.length; i++){
                                        System.out.print(classData[i] + ((i == recordData.length - 1)? "\n" : ", "));
                                    }
                                    System.out.print("record : ");
                                    for(int i = 0; i < recordData.length; i++){
                                        System.out.print(recordData[i] + ((i == recordData.length - 1)? "\n" : ", "));
                                    }
                                    
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("num of refactorings : %d".formatted(numOfClassToRecordRefactoring));
        String[] tags = {"equals(Object o)", "toString()", "hashCode()", "constructor", "getters", "others"};
        for(int i = 0; i < decrease.length; i++){
            System.out.println(tags[i]);
            System.out.println("decrease : %d".formatted(decrease[i]));
            System.out.println("increase : %d".formatted(increase[i]));
            System.out.println("decrease cases: %d".formatted(decreaseCase[i]));
            System.out.println("increase cases: %d".formatted(increaseCase[i]));
            System.out.println("still existing : %d".formatted(existingCase[i]));
            System.out.println();
        }
    }

    public static boolean isEqualsMethod(MethodTree method){
        String parameterType;
        return method.name().toSource("").equals("equals")
            && method.parameters().size() == 1
            && ((parameterType = method.parameters().get(0).actualType().toQualifiedTypeName()).equals("Object")
                || parameterType.equals("java.lang.Object"));
    }

    public static boolean isHashCodeMethod(MethodTree method){
        return method.name().toSource("").equals("hashCode")
            && method.parameters().size() == 0;
    }

    public static boolean isToStringMethod(MethodTree method){
        return method.name().toSource("").equals("toString")
            && method.parameters().size() == 0;
    }

    public static boolean isConstructor(MethodTree method){
        return method.actualType() == null;
    }

    public static boolean isGetterMethod(ClassTree type, MethodTree method){
        if(!method.parameters().isEmpty()){
            return false;
        }
        List<VariableTree> fields = new ArrayList<>();
        if(type.declType() == DeclarationType.CLASS){
            for(Tree t : type.members()){
                if(t instanceof VariableTree v && !v.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                    fields.add(v);
                }
            }
        }else if(type.declType() == DeclarationType.RECORD){
            if(type.recordComponents() == null){
                throw new IllegalArgumentException("Invalid record declaration.");
            }
            fields.addAll(type.recordComponents());
        }
        for(var field : fields){
            var f = field;
            do{
                String fieldName = f.name().toSource("");
                String methodName = method.name().toSource("");
                if(methodName.equals(fieldName) || methodName.equals(replaceToGetterFormat(fieldName))){
                    return true;
                }
            }while((f = f.follows()) != null);
        }
        return false;
    }

    private static String replaceToGetterFormat(String fieldName){
        return "get" + fieldName.substring(0, 1).toUpperCase()
            + ((fieldName.length() > 1)? fieldName.substring(1) : "");
    }

    private static int[] collectMethodData(ClassTree c){
        int[] data = new int[6];
        for(var member : c.members()){
            if(member instanceof MethodTree method){
                if(isEqualsMethod(method)){
                    data[0]++;
                }else if(isToStringMethod(method)){
                    data[1]++;
                }else if(isHashCodeMethod(method)){
                    data[2]++;
                }else if(isConstructor(method)){
                    data[3]++;
                }else if(isGetterMethod(c, method)){
                    data[4]++;
                }else{
                    data[5]++;
                }
            }
        }
        return data;
    }

}
