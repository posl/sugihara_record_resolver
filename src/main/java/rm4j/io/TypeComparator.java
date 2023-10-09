package rm4j.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.MemberReferenceTree;
import rm4j.compiler.tree.MemberSelectTree;
import rm4j.compiler.tree.MethodInvocationTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.VariableTree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.ReturnTree;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.ThisTree;
import rm4j.compiler.tree.Tree.DeclarationType;

public class TypeComparator implements FileManager{

    private static final File DIFF_DIRECTORY = new File("out/rq2-1/diffs");

    public void compareType() throws IOException {
        JavaCompiler compiler = new JavaCompiler();
        PropertyResolver resolver = new PropertyResolver();
        File constructorOverrides = new File("out/rq2-1/constructor_override_cases");
        int caseId = 0;
        int[] data = new int[26];

        deleteAll(constructorOverrides);
        constructorOverrides.mkdirs();

        for(File repository : DIFF_DIRECTORY.listFiles(File::isDirectory)){
            System.out.println("mining commits of %s...".formatted(repository.getName()));
            for(File commitDate : repository.listFiles(File::isDirectory)){

                Set<PropertyInfo> propertyInfos = new HashSet<>();

                for(File filePair : commitDate.listFiles(File::isDirectory)){
                    File before = new File(filePair, "before.java");
                    File after = new File(filePair, "after.java");
                    if(before.exists() && after.exists()){
                        Map<String, ClassTree> oldClassMap = new TypeSet(before, compiler).classes();
                        Map<String, ClassTree> newClassMap = new TypeSet(after, compiler).classes();
                        for(String classPath : oldClassMap.keySet()){
                            ClassTree oldClass = oldClassMap.get(classPath);
                            if(oldClass.declType() == DeclarationType.CLASS){
                                ClassTree correspondingType = newClassMap.get(classPath);
                                if(correspondingType != null && correspondingType.declType() == DeclarationType.RECORD){
                                    data[0]++;

                                    PropertyInfo propertyInfo = new PropertyInfo(oldClass, correspondingType, new HashSet<>());
                                    Map<String, Set<String>> fieldToGetterMapping = getPropertyMap(oldClass);

                                    int numOfConstructors = 0;
                                    int numOfEffectiveGetters = 0;

                                    int[] status = new int[1];

                                    for(Tree member : oldClass.members()){
                                        if(member instanceof MethodTree m){
                                            if(resolver.isConstructor(m)){
                                                data[3]++;
                                                numOfConstructors++;
                                            }else if(resolver.isEqualsMethod(m)){
                                                data[13]++;
                                            }else if(resolver.isHashCodeMethod(m)){
                                                data[14]++;
                                            }else if(resolver.isToStringMethod(m)){
                                                data[15]++;
                                            }else if(resolver.isEffectivelyGetter(m, oldClass)){
                                                data[5]++;
                                                numOfEffectiveGetters++;
                                                if(resolver.isTraditionalFormatGetter(m, oldClass)){
                                                    data[6]++;
                                                }else if(resolver.isRecordFormatGetter(m, oldClass)){
                                                    data[7]++;
                                                }
                                            }
                                        }
                                    }

                                    for(Tree member : correspondingType.members()){
                                        if(member instanceof MethodTree m){
                                            if(resolver.isConstructor(m)){
                                                data[4]++;
                                                numOfConstructors--;
                                                if(resolver.isCompactConstructor(m)){
                                                    status[0] |= 0b10;
                                                }
                                                if(resolver.isCanonicalConstructor(m, correspondingType)){
                                                    status[0] |=0b1000;
                                                }
                                            }else if(resolver.isEqualsMethod(m)){
                                                data[16]++;
                                            }else if(resolver.isHashCodeMethod(m)){
                                                data[17]++;
                                            }else if(resolver.isToStringMethod(m)){
                                                data[18]++;
                                            }else if(resolver.isEffectivelyGetter(m, oldClass)){
                                                data[8]++;
                                                numOfEffectiveGetters--;
                                                if(resolver.isTraditionalFormatGetter(m, oldClass)){
                                                    data[9]++;
                                                }else if(resolver.isRecordFormatGetter(m, oldClass)){
                                                    data[10]++;
                                                }
                                            }
                                        }
                                    }

                                    if(correspondingType.recordComponents() != null){
                                        propertyInfo.nameMappings.addAll(
                                            correspondingType.recordComponents()
                                                .stream()
                                                .<PropertyNameSet>mapMulti((v, consumer) -> {
                                                    String key = v.name().name();
                                                    if(fieldToGetterMapping.get(key) != null){
                                                        fieldToGetterMapping.get(key)
                                                            .stream()
                                                            .<PropertyNameSet>map(s -> new PropertyNameSet(key, s, key))
                                                            .peek(System.out::println).forEach(consumer::accept);
                                                    }
                                                }).toList());
                                    }

                                    
                                    List<String> recordGetterNames = correspondingType.members().stream().<String>mapMulti((t, consumer) -> {
                                        if(t instanceof MethodTree m && resolver.isEffectivelyGetter(m, correspondingType)){
                                            consumer.accept(m.name().name());
                                        }
                                    }).toList();
                                    propertyInfo.nameMappings.stream().forEach(p -> {
                                        if(!recordGetterNames.contains(p.classGetterName)){
                                            data[12]++;
                                            status[0] |= 0b1;
                                            if(p.classGetterName.startsWith("get")){
                                                String withoutPrefix = p.classGetterName.substring(3);
                                                if(withoutPrefix.length() > 0 && 
                                                    p.recordGetterName().equals(withoutPrefix.substring(0, 1).toLowerCase() + withoutPrefix.substring(1))){
                                                    status[0] |= 0b100;
                                                    data[21]++;
                                                }
                                            }
                                        }
                                    });

                                    if(numOfConstructors > 0){
                                        data[1]++;
                                    }else if(numOfConstructors == 0 && (status[0] & 0b10) != 0){
                                        data[2]++;
                                    }

                                    if((status[0] & 0b1000) != 0){
                                        File out = new File(constructorOverrides, "case%d.java".formatted(caseId++));
                                        out.createNewFile();
                                        try(FileReader reader = new FileReader(after);
                                            FileWriter writer = new FileWriter(out)){
                                            reader.transferTo(writer);
                                        }
                                    }

                                    if(numOfEffectiveGetters > 0){
                                        data[19]++;
                                    }

                                    if((status[0] & 0b1) != 0){
                                        data[11]++;
                                    }

                                    if((status[0] & 0b10) != 0){
                                        data[22]++;
                                    }

                                    if((status[0] & 0b100) != 0){
                                        data[20]++;
                                    }
                                    
                                    propertyInfos.add(propertyInfo);
                                }
                            }
                        }
                    }
                }
    
                for(File filePair : commitDate.listFiles()){
                    File before = new File(filePair, "before.java");
                    File after = new File(filePair, "after.java");
                    if(before.exists() && after.exists()){
                        CompilationUnitTree oldSrc = ProjectUnit.parseFile(before, compiler);
                        CompilationUnitTree newSrc = ProjectUnit.parseFile(after, compiler);

                        int[] referenceChanges = countComponents(oldSrc, newSrc, propertyInfos);
                        for(int i = 0; i < 3; i++){
                            if(referenceChanges[i] < 0){
                                referenceChanges[i] = 0;
                            }
                        }

                        boolean flag = false;
                        for(int i = 0; i < 3; i++){
                            if(referenceChanges[i] > 0){
                                flag = true;
                                data[23+i] += referenceChanges[i];
                            }
                        }
                        if(flag){
                            System.out.println("%s : %d, %d, %d".formatted(filePair, referenceChanges[0], referenceChanges[1], referenceChanges[2]));
                        }
                    }
                }
            }
        }

        File out = new File("out/rq2-1/conversion_data.csv");
        out.delete();
        try(FileWriter writer = new FileWriter(out)){
            writer.append("total class -> record refactorings : %d \n".formatted(data[0]));
            writer.append("constructor reduce refactorings : %d (%.1f%%) \n".formatted(data[1], toPercentage(data[1], data[0])));
            writer.append("found compact constructors : %d (%.1f%%) \n".formatted(data[22], toPercentage(data[22], data[0])));
            writer.append("compact constructor refactorings : %d (%.1f%%) \n" .formatted(data[2], toPercentage(data[2], data[0])));
            writer.append("effective getter reduction refactorings : %d (%.1f%%) \n".formatted(data[19], toPercentage(data[19], data[0])));
            writer.append("conversions that changed properties' name: %d (%.1f%%) \n".formatted(data[11], toPercentage(data[11], data[0])));
            writer.append("total number of getters that changed name: %d \n".formatted(data[12]));
            writer.append("removal of get-prefix conversions : %d (%.1f%%) \n".formatted(data[20], toPercentage(data[20], data[0])));
            writer.append("total number of getters being removed get-prefix: %d \n\n".formatted(data[21]));
            writer.append("      properties       | classes | records | delta \n");
            writer.append(" ================================================ \n");
            writer.append("     constructors      |   %d   |   %d   |  %d  \n".formatted(data[3], data[4], data[3] - data[4]));
            writer.append("     real getters      |   %d   |   %d   |  %d  \n".formatted(data[5], data[8], data[5] - data[8]));
            writer.append(" get-prefixed getters  |   %d   |   %d   |  %d  \n".formatted(data[6], data[9], data[6] - data[9]));
            writer.append(" record-format getters |   %d   |   %d   |  %d  \n".formatted(data[7], data[10], data[7] - data[10]));
            writer.append("   equals(Object o)    |   %d   |   %d   |  %d  \n".formatted(data[13], data[16], data[13] - data[16]));
            writer.append("      hashCode()       |   %d   |   %d   |  %d  \n".formatted(data[14], data[17], data[14] - data[17]));
            writer.append("      toString()       |   %d   |   %d   |  %d  \n\n".formatted(data[15], data[18], data[15] - data[18]));
            writer.append("deleted field reference : %d | deleted class getter reference : %d | added record getter reference : %d \n".formatted(data[23], data[24], data[25]));
        }
    }

    public void getRecordToClassConversion() throws IOException {
        JavaCompiler compiler = new JavaCompiler();
        PropertyResolver resolver = new PropertyResolver();
        int data[] = new int[9];
        File toClassConversions = new File("out/rq2-2/to_class_conversions");

        deleteAll(toClassConversions);
        toClassConversions.mkdirs();

        for(File repository : DIFF_DIRECTORY.listFiles(File::isDirectory)){
            for(File commitDate : repository.listFiles(File::isDirectory)){

                for(File filePair : commitDate.listFiles(File::isDirectory)){
                    File before = new File(filePair, "before.java");
                    File after = new File(filePair, "after.java");
                    if(before.exists() && after.exists()){
                        Map<String, ClassTree> oldClassMap = new TypeSet(before, compiler).classes();
                        Map<String, ClassTree> newClassMap = new TypeSet(after, compiler).classes();
                        for(String classPath : oldClassMap.keySet()){
                            ClassTree oldClass = oldClassMap.get(classPath);
                            if(oldClass.declType() == DeclarationType.RECORD){
                                ClassTree correspondingType = newClassMap.get(classPath);
                                if(correspondingType != null && correspondingType.declType() == DeclarationType.CLASS){
                                    int result = resolver.getClassData(correspondingType);
                                    data[0]++;
                                    for(int i = 1; i < 8; i++){
                                        data[i] += (result>>(i-1))&1;
                                    }
                                    if((result & 0b0011111) == 0){
                                        data[8]++;
                                        File commitInfo = new File(commitDate, "commit-info.txt");
                                        try(BufferedReader reader = new BufferedReader(new FileReader(commitInfo))){
                                            String commitId = reader.readLine().split(" ")[1];
                                            File dir = new File(toClassConversions, commitId + ":" + filePair.getName());
                                            if(!dir.exists()){
                                                dir.mkdir();
                                                copyFiles(before, new File(dir, "before.java"));
                                                copyFiles(after, new File(dir, "after.java"));
                                                File changedClasses = new File(dir, "converted_records.txt");
                                                if(!changedClasses.exists()){
                                                    changedClasses.createNewFile();
                                                }
                                                try(FileWriter writer = new FileWriter(changedClasses)){
                                                    writer.append(correspondingType.name().name()+"\n");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        File out = new File("out/rq2-2/converted_record_data.csv");
        out.delete();
        try(FileWriter writer = new FileWriter(out)){
            writer.append("all_conversions : %d \n".formatted(data[0]));
            writer.append("has_non_final_fields : %d (%.1f%%) \n".formatted(data[1], toPercentage(data[1], data[0])));
            writer.append("extends_other_class : %d (%.1f%%) \n".formatted(data[2], toPercentage(data[2], data[0])));
            writer.append("has_prohibited_modifiers : %d (%.1f%%) \n" .formatted(data[3], toPercentage(data[3], data[0])));
            writer.append("has_instrance_initializer : %d (%.1f%%) \n".formatted(data[4], toPercentage(data[4], data[0])));
            writer.append("has_inproper_canonical_constructor: %d (%.1f%%) \n".formatted(data[5], toPercentage(data[5], data[0])));
            writer.append("has_get_prefixed_getters: %d (%.1f%%) \n".formatted(data[6], toPercentage(data[6], data[0])));
            writer.append("has_effective_getters: %d (%.1f%%) \n".formatted(data[7], toPercentage(data[7], data[0])));
            writer.append("none_of_the_above: %d (%.1f%%) \n".formatted(data[8], toPercentage(data[8], data[0])));
        }
    }

    private double toPercentage(int m, int n){
        return m * 100.0 / n;
    }

    private Map<String, Set<String>> getPropertyMap(ClassTree c){
        Map<String, Set<String>> propertyMap = new HashMap<>();
        List<String> fieldNames = c.members().stream().<String>mapMulti(
            (member, consumer) -> {
                if(member instanceof VariableTree v && !v.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                    consumer.accept(v.name().name());
                }
            }
        ).toList();
        c.members().forEach(t -> {
            if(t instanceof MethodTree m && m.body().statements().size() == 1){
                StatementTree stmt = m.body().statements().get(0);
                    if(stmt instanceof ReturnTree r){
                        if(r.expression() instanceof MemberSelectTree ms){
                            if(ms.expression() instanceof ThisTree th
                                && th.qualifier() == ExpressionNameTree.EMPTY
                                && fieldNames.contains(ms.identifier().name())){
                                String key = ms.identifier().name();
                                if(!propertyMap.containsKey(key)){
                                    propertyMap.put(key, new HashSet<>());
                                }
                                propertyMap.get(key).add(m.name().name());
                            }
                        }else if(r.expression() instanceof ExpressionNameTree n){
                            if(n.qualifier() == ExpressionNameTree.EMPTY
                                && fieldNames.contains(n.identifier().name())){
                                String key = n.identifier().name();
                                if(!propertyMap.containsKey(key)){
                                    propertyMap.put(key, new HashSet<>());
                                }
                                propertyMap.get(key).add(m.name().name());
                            }
                        }
                    }
                }
            }
        );
        return propertyMap;
    }

    private List<Tree> enumerateTree(Tree parent){
        Set<TreeCapsule> trees = new HashSet<>();
        List<Tree> children = new LinkedList<>();
        children.add(parent);
        do{
            final int size = children.size();
            for(int i = 0; i < size; i++){
                Tree child = children.get(0);
                child.children().stream().<Tree>mapMulti((t, consumer) -> {
                    if(t instanceof MethodInvocationTree mi){
                        mi.arguments().forEach(consumer::accept);
                        if(mi.methodSelect() instanceof MemberSelectTree ms){
                            consumer.accept(ms.expression());
                        }else if(mi.methodSelect() instanceof ExpressionNameTree ex && ex != ExpressionNameTree.EMPTY){
                            consumer.accept(ex.qualifier());
                        }
                    }else{
                        consumer.accept(t);
                    }
                }).forEach(children::add);
                trees.add(new TreeCapsule(children.remove(0)));
            }
        }while(!children.isEmpty());
        return trees.stream().map(TreeCapsule::tree).toList();
    }

    private int[] countComponents(Tree oldSrc, Tree newSrc, Set<PropertyInfo> propertyInfos){
        int[] count = new int[3];
        BiConsumer<int[], IntUnaryOperator> countReduction = (arr, sign) -> {
            for(int i = 0; i < 3; i++){
                count[i] += sign.applyAsInt(arr[i]);
            }
        };

        countReduction.accept(visitPreviousSource(oldSrc, propertyInfos), p -> p);
        countReduction.accept(visitCurrentSource(newSrc, propertyInfos), p -> p);
        propertyInfos.stream().<ClassTree>map(PropertyInfo::oldClass).distinct().filter(enumerateTree(oldSrc)::contains).forEach(c -> {
            countReduction.accept(visitPreviousSource(c, Set.of(propertyInfos.stream().filter(info -> info.oldClass.equals(c)).toArray(PropertyInfo[]::new))), p -> -p);
        });
        propertyInfos.stream().<ClassTree>map(PropertyInfo::newRecord).distinct().filter(enumerateTree(newSrc)::contains).forEach(c -> {
            countReduction.accept(visitCurrentSource(c, Set.of(propertyInfos.stream().filter(info -> info.newRecord.equals(c)).toArray(PropertyInfo[]::new))), p -> -p);
        });
        return count;
    }

    private int[] visitPreviousSource(Tree src, Set<PropertyInfo> propertyInfos){
        int[] count = new int[3];
        Predicate<Predicate<PropertyNameSet>> tester1 = func -> propertyInfos.stream().anyMatch(info -> info.nameMappings.stream().anyMatch(func));
        Predicate<Predicate<PropertyNameSet>> tester2 = func -> propertyInfos.stream().anyMatch(info -> info.nameMappings.stream().filter(map -> !map.classFieldName.equals(map.classGetterName)).anyMatch(func));
        enumerateTree(src).forEach(t -> {
            if(t instanceof MemberSelectTree ms){
                if(tester1.test(set -> set.classFieldName.equals(ms.identifier().name()))){
                    count[0]++;
                }
            }else if(t instanceof MemberReferenceTree mr){
                if(tester2.test(set -> set.classGetterName.equals(mr.methodName().name()))){
                    count[1]++;
                }else if(tester2.test(set -> set.recordGetterName.equals(mr.methodName().name()))){
                    count[2]--;
                }
            }else if(t instanceof MethodInvocationTree mi){
                if(mi.methodSelect() instanceof MemberSelectTree ms){
                    if(tester2.test(set -> set.classGetterName.equals(ms.identifier().name()))){
                        count[1]++;
                    }else if(tester2.test(set -> set.recordGetterName.equals(ms.identifier().name()))){
                        count[2]--;
                    }
                }else if(mi.methodSelect() instanceof ExpressionNameTree ex && ex != ExpressionNameTree.EMPTY){
                    if(tester2.test(set -> set.classGetterName.equals(ex.identifier().name()))){
                        count[1]++;
                    }else if(tester2.test(set -> set.recordGetterName.equals(ex.identifier().name()))){
                        count[2]--;
                    }
                }
            }
        });
        return count;
    }

    private int[] visitCurrentSource(Tree src, Set<PropertyInfo> propertyInfos){
        int[] count = new int[3];
        Predicate<Predicate<PropertyNameSet>> tester1 = func -> propertyInfos.stream().anyMatch(info -> info.nameMappings.stream().anyMatch(func));
        Predicate<Predicate<PropertyNameSet>> tester2 = func -> propertyInfos.stream().anyMatch(info -> info.nameMappings.stream().filter(map -> !map.classFieldName.equals(map.classGetterName)).anyMatch(func));
        enumerateTree(src).forEach(t -> {
            if(t instanceof MemberSelectTree ms){
                if(tester1.test(set -> set.classFieldName.equals(ms.identifier().name()))){
                    count[0]--;
                }
            }else if(t instanceof MemberReferenceTree mr){
                if(tester2.test(set -> set.recordGetterName.equals(mr.methodName().name()))){
                    count[2]++;
                }else if(tester2.test(set -> set.classGetterName.equals(mr.methodName().name()))){
                    count[1]--;
                }
            }else if(t instanceof MethodInvocationTree mi){
                if(mi.methodSelect() instanceof MemberSelectTree ms){
                    if(tester2.test(set -> set.recordGetterName.equals(ms.identifier().name()))){
                        count[2]++;
                    }else if(tester2.test(set -> set.classGetterName.equals(ms.identifier().name()))){
                        count[1]--;
                    }
                }else if(mi.methodSelect() instanceof ExpressionNameTree ex && ex != ExpressionNameTree.EMPTY){
                    if(tester2.test(set -> set.recordGetterName.equals(ex.identifier().name()))){
                        count[2]++;
                    }else if(tester2.test(set -> set.classGetterName.equals(ex.identifier().name()))){
                        count[1]--;
                    }
                }
            }
        });
        return count;
    }

    private record PropertyNameSet(String classFieldName, String classGetterName, String recordGetterName){

        @Override
        public String toString(){
            return "classField : %s | classGetter : %s | recordProerty : %s ".formatted(classFieldName, classGetterName, recordGetterName);
        }
    }

    private record PropertyInfo(ClassTree oldClass, ClassTree newRecord, Set<PropertyNameSet> nameMappings){}

    private record TreeCapsule(Tree tree){
        @Override public boolean equals(Object o){
            if(o instanceof TreeCapsule tc){
                return tree == tc.tree;
            }
            return false;
        }
    }

}
