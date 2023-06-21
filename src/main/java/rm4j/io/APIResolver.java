package rm4j.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.file.ProjectUnit;
import rm4j.compiler.resolution.PrimitiveType;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExportsTree;
import rm4j.compiler.tree.ImportTree;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.TypeTree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;

public class APIResolver implements Serializable{

    private static final long serialVersionUID = 0xC436E989F898BEEAL;

    private static final File API_PATH = new File("../data_original/JavaAPI17");
    public static final File OBJECT_PATH = new File("work/api17info.ser");

    public final Map<String, APINameUnit> packages = new HashMap<>();

    public static APIResolver deserialize() throws IOException{
        if(OBJECT_PATH.exists()){
            try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(OBJECT_PATH))){
                return (APIResolver)in.readObject();
            }catch(ClassNotFoundException | ClassCastException e){
                e.printStackTrace();
            }
        }
        return new APIResolver();
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
    
    public String getFullyQualifiedName(String typeName, List<ImportTree> imports) {
        String arraySuffix = "";
        int suffixStart =  typeName.indexOf("[");
        if(suffixStart != -1){
            arraySuffix = typeName.substring(suffixStart);
            typeName = typeName.substring(0, suffixStart);
        }
        for(PrimitiveType pt : PrimitiveType.values()){
            if(pt.name().toLowerCase().equals(typeName)){
                return typeName + arraySuffix;
            }
        }
        String ret = searchAPIType(typeName);
        if(!ret.equals("#userType")){
            return ret + arraySuffix;
        }
        String name = typeName.replaceFirst("\\..+", "");
        for(ImportTree imp : imports){
            if(!imp.isOnDemand() && imp.qualifiedName().identifier().name().equals(name)){
                ret = searchAPIType(((TypeTree)imp.qualifiedName().qualifier()).toQualifiedTypeName() + "." + typeName);
                if(!ret.equals("#userType")){
                    return ret + arraySuffix;
                }
            }
        }
        for(ImportTree imp : imports){
            if(imp.isOnDemand()){
                ret = searchAPIType(imp.qualifiedName().toQualifiedTypeName() + "." + typeName);
                if(!ret.equals("#userType")){
                    return ret + arraySuffix;
                }
            }
        }
        return searchAPIType("java.lang." + typeName) + arraySuffix;
    }

    private String searchAPIType(String typeName){
        String name = typeName;
        String ret;
        for(int i = name.lastIndexOf("."); i != -1; i = name.lastIndexOf(".")){
            name = name.substring(0, i);
            APINameUnit unit = packages.get(name);
            if(unit != null){
                ret = name;
                String[] identifiers = typeName.replaceFirst(name + ".", "").split("\\.");
                int j;
                for(j = 0; j < identifiers.length; j++){
                    unit = unit.types.get(identifiers[j]);
                    if(unit == null){
                        break;
                    }
                    ret += "." + identifiers[j];
                }
                if(j == identifiers.length){
                    return ret;
                }
            }
        }
        return "#userType";
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
            if(t instanceof ClassTree c && (c.modifiers().getModifiers().contains(ModifierKeyword.PUBLIC) ||
                type.declType() == DeclarationType.INTERFACE || type.declType() == DeclarationType.ANNOTATION_INTERFACE)){
                types.put(c.name().toSource(""), resolveType(c));
            }
        }
        return new APINameUnit(type.name().toSource(""), types);
    }

    record APINameUnit(String name, Map<String, APINameUnit> types) implements Serializable{
        private static final long serialVersionUID = 0x72061C5D45E05CC7L;
    }

}
