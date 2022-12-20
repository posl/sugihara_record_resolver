package rm4j.io.git;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.Tree;

public class TypeSet {
    
    public final File file;
    public Map<String, ClassTree> classes;
    public Set<ClassTree> records;

    public TypeSet(final File file, final JavaCompiler compiler){
        this.file = file;
        this.records = new HashSet<>();
        CompilationUnitTree tree = ProjectUnit.parseFile(file, compiler);
        try{
            Tree.visit(tree, t -> {
                if(t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                    records.add(c);
                }
            });
        }catch(CompileException e){
            System.out.println(e);
        }
        if(tree != CompilationUnitTree.ERROR){
            Map<String, ClassTree> classes = new HashMap<>();
            for(StatementTree s : tree.typeDecls()){
                if(s instanceof ClassTree c){
                    classes.put(c.name().toString(), c);
                }
            }
            this.classes.putAll(classes);
            boolean flag;
            do{
                flag = false;
                Map<String, ClassTree> buffer = new HashMap<>();
                for(String classPath : classes.keySet()){
                    ClassTree classTree = classes.get(classPath);
                    for(Tree member : classTree.members()){
                        if(member instanceof ClassTree c){
                            buffer.put(classPath + "." + c.name().toSource(""), c);
                            flag = true;
                        }
                    }
                }
                this.classes.putAll(buffer);
                classes = buffer;
            }while(flag);
        }
    }

}
