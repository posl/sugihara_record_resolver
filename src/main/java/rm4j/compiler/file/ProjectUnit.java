package rm4j.compiler.file;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.resolution.Accessible;
import rm4j.compiler.tree.CompilationUnitTree;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.IdentifierTree;
import rm4j.compiler.tree.Tree;
import rm4j.util.functions.CEConsumer;

public final class ProjectUnit{
    public static final boolean PARALLEL = false;

    volatile int succeed = 0;
    volatile int failed = 0;

    public static volatile int numOfFailedFiles = 0;
    public static volatile int numOfFiles = 0;
    private static final File API = new File("../../lib/JavaAPI17");

    private final JavaModule unnamedModule = new JavaModule(CompilationUnitTree.UNNAMED_MODULE);
    private final JavaPackage unnamedPackage = new JavaPackage(ExpressionNameTree.EMPTY, new ConcurrentHashMap<>());

    private final Map<ExpressionNameTree, JavaModule> moduleTable = new ConcurrentHashMap<>();
    private final Map<ExpressionNameTree, JavaPackage> packageTable = new ConcurrentHashMap<>();
    private final JavaCompiler compiler = new JavaCompiler();

   {
        moduleTable.put(ExpressionNameTree.EMPTY, unnamedModule);
        packageTable.put(ExpressionNameTree.EMPTY, unnamedPackage);
    }

    public ProjectUnit(CEConsumer<? super Tree> query){
        Arrays.asList(API.listFiles(dir -> dir.isDirectory())).parallelStream().forEach(dir ->{
            resolvePackage(dir, ExpressionNameTree.EMPTY, unnamedModule, query);
        });
        // System.out.println(String.format("Failed parsing in %d files. (in %d)",
        // failed, succeed + failed));
    }

    public ProjectUnit(File sourcePath, CEConsumer<? super Tree> query){
        // System.out.println(sourcePath);
        // Arrays.asList(API.listFiles(dir ->
        // dir.isDirectory())).parallelStream().forEach(dir ->{resolvePackage(dir,
        // ExpressionNameTree.EMPTY, unnamedModule);});

        if (sourcePath.isDirectory()){
            resolvePackage(sourcePath, ExpressionNameTree.EMPTY, unnamedModule, query);
        }
        // System.out.println(String.format("Failed parsing in %d files. (in %d)",
        // failed, succeed + failed));
    }

    private JavaPackage resolvePackage(File dir, ExpressionNameTree name, JavaModule associated,
            CEConsumer<? super Tree> query){

        for (File f : dir.listFiles((d, s) -> s.equals("module-info.java"))){
            CompilationUnitTree moduleSpecification = compiler.compile(f);
            if (moduleSpecification != CompilationUnitTree.ERROR && moduleSpecification.module() != null){
                associated = new JavaModule(moduleSpecification);
                moduleTable.put(associated.qualifiedName(), associated);
                numOfFailedFiles++;
            }else{
                numOfFiles++;
            }
        }

        Map<IdentifierTree, Accessible> contents = new ConcurrentHashMap<>();

        final JavaModule module = associated;
        Consumer<File> ex = f ->{
            if (f.isDirectory()){
                var directoryName = new IdentifierTree(f.getName());
                JavaPackage subPackage = resolvePackage(f, new ExpressionNameTree(name, directoryName), module, query);
                contents.put(directoryName, subPackage);
                module.associatePackage(subPackage);
            }else{
                String fileName = f.getName();
                int separator = fileName.lastIndexOf(".");
                if (separator > 0 && fileName.substring(separator).equals(".java")){
                    var sourceName = new IdentifierTree(fileName.substring(0, separator));
                    var manager = new JavaSourceManager(new ExpressionNameTree(name, sourceName), compiler.compile(f));
                    contents.put(sourceName, manager);
                    if (manager.source() == CompilationUnitTree.ERROR){
                        failed++;
                        numOfFailedFiles++;
                    }else{
                        try{
                            Tree.visit(manager.source(), query);
                        }catch (CompileException e){
                            System.out.println(e);
                        }
                        synchronized (this){
                            succeed++;
                        }
                    }
                    synchronized (this){
                        numOfFiles++;
                    }

                }
            }
        };
        List<File> files = Arrays.asList(dir.listFiles((d, s) -> !s.equals("module-info.java")));
        if (PARALLEL){
            files.parallelStream().forEach(ex);
        }else{
            files.forEach(ex);
        }

        JavaPackage p = packageTable.get(name);
        if (p == null){
            p = new JavaPackage(name, contents);
        }else{
            p.accept(contents);
        }
        return p;
    }

    public static CompilationUnitTree parseFile(final File file, final JavaCompiler compiler){
        if (file.getName().endsWith(".java")){
           return compiler.compile(file);
        }
        return null;
    }

}
