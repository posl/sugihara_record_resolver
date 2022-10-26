package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;

public record ElementValuePairTree(IdentifierTree identifier, ExpressionTree elementValue) implements ExpressionTree, Accessor{

    static ElementValuePairTree parse(JavaTokenManager src) throws CompileException{
        IdentifierTree identifier;
        if(src.match(1, JavaTS.SIMPLE_ASSIGNMENT)){
            identifier = IdentifierTree.parse(src);
            src.skip(JavaTS.SIMPLE_ASSIGNMENT);
        }else{
            identifier = IdentifierTree.EMPTY;
        }
        return new ElementValuePairTree(identifier, resolveElementValue(src));
    }

    static ExpressionTree resolveElementValue(JavaTokenManager src) throws CompileException{
        if(src.match(JavaTS.LEFT_CURLY_BRACKET)){
            return NewArrayTree.parseArrayInitializer(ElementValuePairTree::resolveElementValue, src);
        }else if(src.match(JavaTS.AT_SIGN)){
            return AnnotationTree.parse(src);
        }else{
            return ExpressionTree.resolveConditionalExpression(src);
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(identifier);
        children.add(elementValue);
        return children;
    }

}