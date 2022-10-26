package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node to declare a new instance of a class.
 *
 * For example:
 * <pre>
 *   new <em>identifier</em> ( )
 *
 *   new <em>identifier</em> ( <em>arguments</em> )
 *
 *   new <em>typeArguments</em> <em>identifier</em> ( <em>arguments</em> )
 *       <em>classBody</em>
 *
 *   <em>enclosingExpression</em>.new <em>identifier</em> ( <em>arguments</em> )
 * </pre>
 *
 * @jls 15.9 Class Instance Creation Expressions
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record NewClassTree(ExpressionTree enclosingExpression, ArrayList<TypeTree> typeArguments, TypeTree createdClass, ArrayList<ExpressionTree> arguments, ClassTree classBody) implements ExpressionTree, Accessor{

    static NewClassTree parse(ExpressionTree enclosingExpression, ArrayList<TypeTree> typeArguments, TypeTree createdClass, JavaTokenManager src) throws CompileException{
        return new NewClassTree(enclosingExpression, typeArguments, createdClass, ExpressionTree.resolveArguments(src), 
            src.match(JavaTS.LEFT_CURLY_BRACKET)? ClassTree.parse(createdClass, src) : null);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        if(enclosingExpression != null){
            children.add(enclosingExpression);
        }
        children.addAll(typeArguments);
        children.addAll(arguments);
        if(classBody != null){
            children.add(classBody);
        }
        return children;
    }

}