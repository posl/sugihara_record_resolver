package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.resolution.Accessor;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a type, expression, or package name.
 *
 * For example:
 * <pre>
 * 
 *    <em>identifier</em> . <em>identifier</em>
 * 
 * </pre>
 * 
 * @author me
 * 
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record NameTree(Accessor qualifier, IdentifierTree identifier) implements Accessor, TypeTree{

    private static NameTree parse(Accessor qualifier, JavaTokenManager src) throws CompileException{
        return new NameTree(qualifier, IdentifierTree.parse(src));
    }

    static TypeTree resolveTypeOrName(JavaTokenManager src) throws CompileException{
        TypeTree type = resolveNonArrayTypeOrName(src);
        return resolveDims(type, src);
    }

    static TypeTree resolveNonArrayTypeOrName(JavaTokenManager src) throws CompileException{
        TypeTree type = ExpressionNameTree.EMPTY;
        while(true){
            type = resolveSimpleNameType(type, src);
            if(src.match(JavaTS.PERIOD) && (src.match(1, IDENTIFIERS) || src.match(1, JavaTS.AT_SIGN))){
                src.skip(JavaTS.PERIOD);
            }else{
                break;
            }
        }
        return type;
    }

    static TypeTree resolveSimpleNameType(TypeTree type, JavaTokenManager src) throws CompileException{
        var annotations = Tree.resolveAnnotations(src);
        if(src.match(JavaTS.VAR)){
            type = VarTree.parse(src);
        }else if(src.match(IDENTIFIERS)){
            if(type instanceof Accessor a){
                type = parse(a, src);
                if(src.match(JavaTS.LESS_THAN)){
                    type = new ParameterizedTypeTree(a, Tree.resolveTypeArguments(src));
                }
            }else{
                throw new ParserException(String.format("\"%s\" cannot be a qualifier.", type.toString()));
            }
        }else{
            if(type != ExpressionNameTree.EMPTY){
                throw new IllegalTokenException(src.lookAhead(), "reference type name");
            }
            if(src.match(PRIMITIVE_TYPES)){
                type = PrimitiveTypeTree.parse(src);
            }else if(src.match(JavaTS.VOID)){
                type = VoidTree.parse(src);
            }else{
                throw new IllegalTokenException(src.lookAhead(), "type name");
            }
        }
        if(!annotations.isEmpty()){
            return new AnnotatedTypeTree(annotations, type);
        }
        return type;
    }


    static TypeTree resolveDims(TypeTree type, JavaTokenManager src) throws CompileException{
        ArrayList<AnnotationTree> annotations;
        while(followsDims(src)){
            annotations = Tree.resolveAnnotations(src);
            if(src.match(JavaTS.LEFT_SQUARE_BRACKET, JavaTS.RIGHT_SQUARE_BRACKET)){
                type = ArrayTypeTree.parse(type, src);
                if(!annotations.isEmpty()){
                    type = new AnnotatedTypeTree(annotations, type);
                }
            }
        }
        return type;
    }

    static boolean followsDims(JavaTokenManager src) throws CompileException{
        var ptr = src.getPointer();
        return LookAheadMode.ANNOTATIONS.skip(ptr)
                && ptr.match(JavaTS.LEFT_SQUARE_BRACKET, JavaTS.RIGHT_SQUARE_BRACKET);
    }

    public String toSource(String indent){
        return (qualifier == ExpressionNameTree.EMPTY)?
                identifier.name() : qualifier.toSource(indent)+"."+identifier.name(); 
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(qualifier);
        children.add(identifier);
        return children;
    }
}

