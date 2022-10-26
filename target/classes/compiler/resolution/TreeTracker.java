package rm4j.compiler.resolution;

import java.util.ArrayList;
import java.util.Map;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.file.JavaPackage;
import rm4j.compiler.tree.*;
import rm4j.util.SimpleStack;
import rm4j.util.functions.CEFunction;

public class TreeTracker{
    
    public final SimpleStack<Scope> trace = new SimpleStack<>();
    public final ArrayList<ImportTree> imports;

    public TreeTracker(CompilationUnitTree unit, Map<ExpressionNameTree, JavaPackage> packageTable){
        this.imports = unit.imports();
        trace.push(new Scope(unit));
    }

    public TypeIdentifier searchType(IdentifierTree id) throws CompileException{
        return search(s -> s.searchType(id));
    }

    public ExpressionIdentifier searchExpression(IdentifierTree id) throws CompileException{
        return search(s -> s.searchExpression(id));
    }

    public MethodTree searchMethod(IdentifierTree id, ArrayList<Type> argumentTypes) throws CompileException{
        return search(s -> s.searchMethod(id, argumentTypes));
    }

    public LabeledStatementTree searchLabel(IdentifierTree id) throws CompileException{
        return search(s -> s.searchLabel(id));
    }

    public <T> T search(CEFunction<Scope, T> getter) throws CompileException{
        var traceCpy = trace.clone();
        T ret;
        while(!traceCpy.isEmpty()){
            ret = getter.apply(traceCpy.pop());
            if(ret != null){
                return ret;
            }
        }
        throw new ParserException("Failed to resolve an identifier.");
    }

    public void acceptLocalType(TypeIdentifier type){
        trace.getTopToken().localInnerTypes.put(type.simpleName(), type);
    }

    public void acceptLocalExpression(ExpressionIdentifier expression){
        trace.getTopToken().localInnerVariables.put(expression.simpleName(), expression);
    }

    public static void resolveImports(){

    }

}
