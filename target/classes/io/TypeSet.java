package rm4j.io;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.Tree.DeclarationType;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.Tree;

public class TypeSet implements Serializable{
    
    private static final long serialVersionUID = 0x68F97D7724C38510L;
    public final File path;
    public final boolean fileExists;
    public Map<String, ClassTree> classes;
    public List<ClassTree> records;

    public TypeSet(final File file, final JavaCompiler compiler){
        this.path = file;
        this.fileExists = file.exists();
        if(fileExists){
            this.classes = new HashMap<>();
            this.records = new ArrayList<>();
            CompilationUnitTree tree = ProjectUnit.parseFile(file, compiler);
            if(tree != CompilationUnitTree.ERROR){
                try{
                    Tree.visit(tree, t -> {
                        if(t instanceof ClassTree c && c.declType() == DeclarationType.RECORD){
                            records.add(c);
                        }
                    });
                }catch(CompileException e){
                    System.out.println(e);
                }
                Map<String, ClassTree> classes = new HashMap<>();
                for(StatementTree s : tree.typeDecls()){
                    if(s instanceof ClassTree c){
                        classes.put(c.name().toSource(""), c);
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
}
