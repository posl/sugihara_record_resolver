package rm4j.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.Tree;
import rm4j.io.APIResolver;
import rm4j.io.PropertyResolver;

import java.util.ArrayList;

public class Sample{

    public static void main(String[] args){
        try{
            PropertyResolver resolver = new PropertyResolver();
            APIResolver typeResolver = APIResolver.deserialize();
            JavaCompiler compiler = new JavaCompiler();
            CompilationUnitTree tree = ProjectUnit.parseFile(new File("src/main/java/rm4j/test/Sample.java"), compiler);
            Tree.visit(tree, t -> {
                if(t instanceof ClassTree c){
                    for(Tree member : c.members()){
                        if(member instanceof MethodTree m){
                            if(resolver.isConstructor(m)){
                                System.out.println("\"%s.%s\" is a constructor.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isCanonicalConstructor(m, c)){
                                System.out.println("\"%s.%s\" is a canonical constructor.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isEffectivelyGetter(m, c)){
                                System.out.println("\"%s.%s\" is effectively a getter.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isEqualsMethod(m)){
                                System.out.println("\"%s.%s\" is an equals(Object o) method.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isHashCodeMethod(m)){
                                System.out.println("\"%s.%s\" is a hashCode() method.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isToStringMethod(m)){
                                System.out.println("\"%s.%s\" is an toString() method.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isRecordFormatGetter(m, c)){
                                System.out.println("\"%s.%s\" is a record-format getter.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isTraditionalFormatGetter(m, c)){
                                System.out.println("\"%s.%s\" is a traditional-format getter.".formatted(c.name().name(), m.name().name()));
                            }
                            if(resolver.isNonImplicitMethod(m, c)){
                                System.out.println("\"%s.%s\" is not implicitly driven in records.".formatted(c.name().name(), m.name().name()));
                            }
                        }
                    }
                }
            });
        }catch(CompileException | IOException e){
            e.printStackTrace();
        }
    }

    private final int x;
    private final int y;

    Sample(int x, int y) throws RuntimeException{
        this.x = x;
        this.y = y;
    }

    int x(){
        return this.x;
    }

    int y(){
        return y;
    }

    public int a(){
        return 0;
    }

    public boolean equals(){
        return false;
    }

    public String toString(String indent){
        return "";
    }

    public int hashCode(int base){
        return base;
    }

}

class Sample2{

    private final int x;
    private final int y;

    Sample2(int x, int y){
        this.x = x;
        this.y = y;
    }

    class P{
        public static boolean b = false;
    }
    int x(){
        return 0;
    }

    boolean y(){
        return P.b;
    }

    public int a(){
        return 0;
    }

    public boolean equals(Object o){
        return false;
    }

    public String toString(){
        return "";
    }

    public int hashCode(){
        return 0;
    }

}

record Sample3(int a){

    Sample3{}

    public int getA(){
        return a;
    }

}
