package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.resolution.Accessor;

/**
 * A tree node for a method invocation expression.
 *
 * For example:
 * <pre>
 *   <em>identifier</em> ( <em>arguments</em> )
 *
 *   this . <em>typeArguments</em> <em>identifier</em> ( <em>arguments</em> )
 * </pre>
 *
 * @jls 15.12 Method Invocation Expressions
 *
 * @author me
 */

public record MethodInvocationTree(ArrayList<TypeTree> typeArguments, ExpressionTree methodSelect, ArrayList<ExpressionTree> arguments) implements ExpressionTree, Accessor{

    static MethodInvocationTree parse(ExpressionTree methodSelect, JavaTokenManager src) throws CompileException{
        return new MethodInvocationTree(new ArrayList<>(), methodSelect, ExpressionTree.resolveArguments(src));
    }

    static MethodInvocationTree parse(Tree qualifier, ArrayList<TypeTree> typeArguments, JavaTokenManager src) throws CompileException{
        ExpressionTree methodSelect = switch(src.lookAhead().resolution){
            case THIS -> ThisTree.parse(qualifier, src);
            case SUPER -> SuperTree.parse(qualifier, src);
            default ->{
                if(qualifier instanceof ExpressionNameTree e){
                    yield ExpressionNameTree.parse(e, src);
                }else if(qualifier instanceof NameTree n){
                    yield ExpressionNameTree.parse(n, src);
                }else if(qualifier instanceof ExpressionTree e){
                    yield MemberSelectTree.parse(e, src);
                }else{
                    throw new ParserException("Illegal form of method invocation.");
                }
            }
        };
        return new MethodInvocationTree(typeArguments, methodSelect, ExpressionTree.resolveArguments(src));
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(typeArguments);
        children.add(methodSelect);
        children.addAll(arguments);
        return children;
    }
    
}
