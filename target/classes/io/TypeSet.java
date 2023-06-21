package rm4j.io;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.BlockTree;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.LambdaExpressionTree;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.Tree;

public class TypeSet implements Serializable{

    private static final long serialVersionUID = 0x68F97D7724C38510L;
    public final File path;
    public final boolean fileExists;
    public final Map<String, ClassTree> classes = new HashMap<>();

    public static void main(String[] args){
        TypeSet set = new TypeSet(new File("src/main/java/rm4j/test/Test.java"), new JavaCompiler());
        for(String s : set.classes.keySet()){
            System.out.println(s);
        }
    }

    public TypeSet(final File file, final JavaCompiler compiler){
        this.path = file;
        this.fileExists = file.exists();

        if(fileExists){
            CompilationUnitTree tree = ProjectUnit.parseFile(file, compiler);
            if(tree != CompilationUnitTree.ERROR){
                Map<String, ClassTree> classes = new HashMap<>();

                for(StatementTree s : tree.typeDecls()){
                    if(s instanceof ClassTree c){
                        classes.put(c.name().toSource(""), c);
                    }
                }

                this.classes.putAll(classes);

                Set<String> paths = new HashSet<>();
                paths.addAll(this.classes.keySet());

                Map<String, Tree> exps = new HashMap<>();
                boolean flag;
                do{
                    flag = false;
                    Map<String, ClassTree> classBuffer = new HashMap<>();
                    Map<String, Tree> expBuffer = new HashMap<>();

                    for(String classPath : classes.keySet()){
                        ClassTree classTree = classes.get(classPath);
                        for(Tree member : classTree.members()){
                            String path;
                            if(member instanceof ClassTree c){
                                path = classPath + "." + c.name().toSource("");
                                classBuffer.put(path, c);
                                paths.add(path);
                                flag = true;
                            }else if(member instanceof BlockTree b){
                                path = classPath + "." + (b.isStatic()? "<clinit>" : "<init>");
                                expBuffer.put(path, b);
                                paths.add(path);
                                flag = true;
                            }else{
                                try{
                                    for(Tree t : Tree.extractChildren(member, t ->
                                        t instanceof LambdaExpressionTree || t instanceof ClassTree)){
                                        if(t instanceof ClassTree c){
                                            int number = 1;
                                            IntFunction<String> pathFunc = n -> classPath + ".#inner$%d".formatted(n) + c.name().name();
                                            while(paths.contains(path = pathFunc.apply(number))){
                                                number++;
                                            }
                                            classBuffer.put(path, c);
                                            paths.add(path);
                                            flag = true;
                                        }else if(t instanceof LambdaExpressionTree l){
                                            int number = 0;
                                            IntFunction<String> pathFunc = n -> classPath + ".#lambda$%d".formatted(n);
                                            while(paths.contains(path = pathFunc.apply(number))){
                                                number++;
                                            }
                                            expBuffer.put(path, l.body());
                                            paths.add(path);
                                            flag = true;
                                        }
                                    }
                                }catch(CompileException e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    for(String expPath: exps.keySet()){
                        try{
                            for(Tree t : Tree.extractChildren(exps.get(expPath), t ->
                                t instanceof LambdaExpressionTree || t instanceof ClassTree)){
                                String path;
                                if(t instanceof ClassTree c){
                                    int number = 1;
                                    IntFunction<String> pathFunc = n -> expPath + ".#inner$%d".formatted(n) + c.name().name();
                                    while(paths.contains(path = pathFunc.apply(number))){
                                        number++;
                                    }
                                    classBuffer.put(path, c);
                                    paths.add(path);
                                    flag = true;
                                }else if(t instanceof LambdaExpressionTree l){
                                    int number = 0;
                                    IntFunction<String> pathFunc = n -> expPath + ".#lambda$%d".formatted(n);
                                    while(paths.contains(path = pathFunc.apply(number))){
                                        number++;
                                    }
                                    expBuffer.put(path, l.body());
                                    paths.add(path);
                                    flag = true;
                                }
                            }
                        }catch(CompileException e){
                            e.printStackTrace();
                        }
                    }

                    this.classes.putAll(classBuffer);
                    classes = classBuffer;
                    exps = expBuffer;
                }while(flag);
            }
        }
    }

    public Map<String, ClassTree> classes(){
        return classes;
    }

}
