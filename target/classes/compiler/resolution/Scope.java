package rm4j.compiler.resolution;

import java.util.ArrayList;
import java.util.HashMap;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.EnumConstantTree;
import rm4j.compiler.tree.IdentifierTree;
import rm4j.compiler.tree.LabeledStatementTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.Tree;

public class Scope{
    
    private final Tree target;
    final HashMap<IdentifierTree, TypeIdentifier> localInnerTypes = new HashMap<>();
    final HashMap<IdentifierTree, ExpressionIdentifier> localInnerVariables = new HashMap<>();

    public Scope(Tree target){
        this.target = target;
    }

    public TypeIdentifier searchType(IdentifierTree id) throws CompileException{
        return null;
    }

    public ExpressionIdentifier searchExpression(IdentifierTree id) throws CompileException{
        return null;
    }

    public MethodTree searchMethod(IdentifierTree id, ArrayList<Type> argumentTypes) throws CompileException{
        if(target instanceof ClassTree c){

        }else if(target instanceof EnumConstantTree e){
            
        }
        return null;
    }

    public LabeledStatementTree searchLabel(IdentifierTree id) throws CompileException{
        if(target instanceof LabeledStatementTree l && l.simpleName().equals(id)){
            return l;
        }else if(target instanceof MethodTree){
            throw new ParserException(String.format("Label \"%s\" cannot be resolved.", id.toString()));
        }
        return null;
    }

}
