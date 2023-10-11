package rm4j.compiler.tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaCompiler;
import rm4j.compiler.core.JavaLexer;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.tokens.Token;
import rm4j.test.Test.IOConsumer;
import rm4j.util.functions.CEFunction;

public class TreeDriver{

    private static final String TEST_FOLDER_PATH = "./org/posl/test/testcases/";
    private static final JavaLexer LEXER = new JavaLexer();

    static final Test[][] TEST_SET ={
        // constructTestSet("EX_UO", "tests parsing unary operators", ExpressionTree::parse),
        // constructTestSet("EX_BO", "tests parsing binary operators", ExpressionTree::parse),
        // constructTestSet("AN", "tests parsing annotations", AnnotationTree::parse),
        // constructTestSet("WC", "tests parsing wildcards", WildcardTree::parse),
        // constructTestSet("T", "tests parsing type names", NameTree::resolveTypeOrName),
        // constructTestSet("PKG", "tests parsing package declarations", PackageTree::parse),
        // constructTestSet("IM", "tests parsing import declarations", ImportTree::parse),
        // constructTestSet("EX_MI", "tests parsing method invocations", ExpressionTree::parse),
        // constructTestSet("EX_AR", "tests parsing array access expressions", ExpressionTree::parse),
        // constructTestSet("EX_AS", "tests parsing assignments", ExpressionTree::parse),
        // constructTestSet("EX_EN", "tests parsing expression names", ExpressionTree::parse),
        // constructTestSet("EX_NC", "tests parsing class instance creations", ExpressionTree::parse),
        // constructTestSet("EX_NA", "tests parsing array creations", ExpressionTree::parse),
        // constructTestSet("EX_CL", "tests parsing class literals", ExpressionTree::parse),
        // constructTestSet("EX_MR", "tests parsing method references", ExpressionTree::parse),
        // constructTestSet("EX_LIT", "tests parsing literals", ExpressionTree::parse),
        // constructTestSet("EX_TH", "tests parsing this expressions", ExpressionTree::parse),
        // constructTestSet("EX_MS", "tests parsing member select expressions", ExpressionTree::parse),
        // constructTestSet("EX_PR", "tests parsing parenthesized expressions", ExpressionTree::parse),
        // constructTestSet("EX_L", "tests parsing lambda expressions", ExpressionTree::parse),
        // constructTestSet("EX_C", "tests parsing conditional expressions", ExpressionTree::parse),
        // constructTestSet("ST_V", "tests parsing variable declarations", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_AS", "tests parsing assert statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_BR", "tests parsing break statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_CTN", "tests parsing continue statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_RT", "tests parsing return statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_YLD", "tests parsing yield statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_THR", "tests parsing throw statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_EX", "tests parsing expression statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_BLK", "tests parsing blocks", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_DW", "tests parsing do statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_SYN", "tests parsing synchronized blocks", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_TR", "tests parsing try statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_SW", "tests parsing switch statements", StatementTree::resolveBlockStatement),
        // constructTestSet("EX_SW", "tests parsing switch expressions", SwitchExpressionTree::parse),
        // constructTestSet("ST_IF", "tests parsing if statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_WH", "tests parsing while statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_FOR", "tests parsing for statements", StatementTree::resolveBlockStatement),
        // constructTestSet("ST_LBL", "tests parsing labeled statements", StatementTree::resolveBlockStatement),
    };

    public void drive(IOConsumer<String> output){
        try{
            output.accept("-----------------------------\n");
            for(Test[] set : TEST_SET){
                driveSet(set, output);
            }
            File dir = new File("../../dataset/jdk17modules/java.base/java/util/");
            for(File f : dir.listFiles()){
                output.accept(Test.instantRun(f)+"\n");
            }
            output.accept("-----------------------------\n");
        }catch(IOException e){
            System.out.println(e);
        }
        
    }

    public void driveSet(Test[] tests, IOConsumer<String> output){
        try{
            for(Test t : tests){
                output.accept(t.run()+"\n");
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }

    private static Test[] constructTestSet(String uniLabel, String content, CEFunction<JavaTokenManager, Tree> aimFunc){
        ArrayList<Test> testList = new ArrayList<>();
        for(int i = 1;;i++){
            String label = uniLabel + "_" + i;
            File f = new File(TEST_FOLDER_PATH + label + ".txt");
            if(f.exists()){
                testList.add(new Test(label, content, aimFunc));
                continue;
            }
            break;
        }
        Test[] testArray = new Test[testList.size()];
        for(int i = 0; i < testArray.length; i++){
            testArray[i] = testList.get(i);
        }
        return testArray;
    }

    private record Test(String label, String content, CEFunction<JavaTokenManager, Tree> aimFunc){

        public String run(){
            String status = "\u001b[00;31m\u001b[1mFail\u001b[00;37m\u001b[0m";
            String executionStatus = "";
            File file = new File(TEST_FOLDER_PATH + label + ".txt");
            JavaTokenManager src;
            Tree parsed = null;
            try{
                src = new JavaTokenManager(LEXER.run(file));
                try{
                    parsed = aimFunc.apply(src);
                    Token t = src.read();
                    if(t.text.equals(label)){
                        status = "\u001b[00;32m\u001b[1mSucceed \u001b[00;37m\u001b[0m";
                    }
                    executionStatus = String.format("expected : result = %s : %s", label, t.text);
                }catch(ParserException e){
                    executionStatus = e.toString();
                    e.printStackTrace();
                }
            }catch(IOException | CompileException e){
                executionStatus = "Lexical analyzation fault.";
            }
            return (JavaTokenManager.ENABLE_PARSING_TRACE? "\n" : "") + String.format("%s ... %s\ndescription : %s\n%s\n", label, status, content, executionStatus);
        }

        public static String instantRun(File file){
            if(file.isDirectory()){
                return String.format("%s ... directory\n", file.getName());
            }
            String status = "\u001b[00;31m\u001b[1mFail\u001b[00;37m\u001b[0m";
            String executionStatus = "";
            Tree parsed = null;
            var compiler = new JavaCompiler();
            CompilationUnitTree top = compiler.compile(file);
            if(top != CompilationUnitTree.ERROR){
                status = "\u001b[00;32m\u001b[1mSucceed \u001b[00;37m\u001b[0m";
                executionStatus = String.format("Succeeded compiling.");
            }else{
                executionStatus = String.format("Failed compiling.");
            }
            return (JavaTokenManager.ENABLE_PARSING_TRACE? "\n" : "") + String.format("%s ... %s\n%s\n", file.getName(), status, executionStatus);
        }

    }
}
