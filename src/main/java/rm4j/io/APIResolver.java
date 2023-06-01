package rm4j.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExportsTree;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;

public class APIResolver implements Serializable{

    private static final long serialVersionUID = 0xC436E989F898BEEAL;

    private static final File API_PATH = new File("../data_original/JavaAPI17");
    
    public static final File OBJECT_PATH = new File("./work/api17info.ser");
    public final Map<String, APINameUnit> packages = new HashMap<>();

    public static APIResolver deserialize() throws IOException{
        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(OBJECT_PATH))){
            return (APIResolver)in.readObject();
        }catch(ClassNotFoundException | ClassCastException e){
            throw new IOException(e);
        }
    }

    public static void main(String[] args){
        try{
            var api = new APIResolver();
            for(var e : api.packages.entrySet()){
                System.out.println(e);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public APIResolver() throws IOException{
        JavaCompiler compiler = new JavaCompiler();
        for(File module : API_PATH.listFiles()){
            if(module.isDirectory()){
                File moduleInfo = new File(module, "module-info.java");
                CompilationUnitTree src = ProjectUnit.parseFile(moduleInfo, compiler);
                if(src == CompilationUnitTree.ERROR || src.module() == null){
                    throw new IOException("Unable to parse %s .".formatted(moduleInfo));
                }else{
                    for(var directive : src.module().directives()){
                        if(directive instanceof ExportsTree export){
                            if(export.moduleNames().isEmpty()){
                                String packageName = export.packageName().toSource("");
                                this.packages.put(packageName, resolvePackage(packageName, module, compiler));
                            }
                        }
                    }
                }
            }
        }
        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(OBJECT_PATH))){
            out.writeObject(this);
        }
    }

    private APINameUnit resolvePackage(String packageName, File module, JavaCompiler compiler) throws IOException{
        File pkgdir = new File(module, packageName.replaceAll("\\.", "/"));
        Map<String, APINameUnit> types = new HashMap<>();
        if(pkgdir.exists() && pkgdir.isDirectory()){
            for(File classFile : pkgdir.listFiles()){
                if(classFile.isFile() && classFile.getName().endsWith(".java")){
                    CompilationUnitTree src = ProjectUnit.parseFile(classFile, compiler);
                    for(StatementTree stmt : src.typeDecls()){
                        if(stmt instanceof ClassTree c && c.modifiers().getModifiers().contains(ModifierKeyword.PUBLIC)){
                            types.put(c.name().toSource(""), resolveType(c));
                        }
                    }
                }
            }
        }
        return new APINameUnit(packageName, types);
    }

    private APINameUnit resolveType(ClassTree type){
        Map<String, APINameUnit> types = new HashMap<>();
        for(Tree t : type.members()){
            if(t instanceof ClassTree c && c.modifiers().getModifiers().contains(ModifierKeyword.PUBLIC)){
                types.put(c.name().toSource(""), resolveType(c));
            }
        }
        return new APINameUnit(type.name().toSource(""), types);
    }

    record APINameUnit(String name, Map<String, APINameUnit> types) implements Serializable{
        private static final long serialVersionUID = 0x72061C5D45E05CC7L;
    }

}
