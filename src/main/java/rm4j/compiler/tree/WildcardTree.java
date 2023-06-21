package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a wildcard type argument.
 * Use{@link #getKind getKind}to determine the kind of bound.
 *
 * For example:
 * <pre>
 *   ?
 *
 *   ? implements <em>bound</em>
 *
 *   ? super <em>bound</em>
 * </pre>
 *
 * @jls 4.5.1 Type Arguments of Parameterized Types
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record WildcardTree(ArrayList<AnnotationTree> annotations, WildcardType wildcardType, TypeTree bound) implements TypeTree{

    public enum WildcardType{
        WILD,
        CONVARIANT,
        CONTRAVARIANT;
    }

    static WildcardTree parse(JavaTokenManager src) throws CompileException{
        ArrayList<AnnotationTree> annotations = Tree.resolveAnnotations(src); 
        src.skip(JavaTS.QUESTION);
        if(src.match(JavaTS.EXTENDS)){
            src.skip(JavaTS.EXTENDS);
            return new WildcardTree(annotations, WildcardType.CONVARIANT, NameTree.resolveTypeOrName(src));
        }else if(src.match(JavaTS.SUPER)){
            src.skip(JavaTS.SUPER);
            return new WildcardTree(annotations, WildcardType.CONTRAVARIANT, NameTree.resolveTypeOrName(src));
        }else{
            return new WildcardTree(annotations, WildcardType.WILD, null);
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(annotations);
        if(bound != null){
            children.add(bound);
        }
        return children;
    }

    @Override
    public String toSource(String indent){
        return Tree.listToSource(annotations, " ", indent) + (annotations.isEmpty()? "" : " ") + switch(wildcardType){
            case WILD -> "?";
            case CONVARIANT -> "? extends " + bound.toSource(indent);
            case CONTRAVARIANT -> "? super " + bound.toSource(indent);
        };
    }

    @Override
    public String toQualifiedTypeName() {
        return switch(wildcardType){
            case WILD -> "?";
            case CONVARIANT -> "? extends " + bound.toQualifiedTypeName();
            case CONTRAVARIANT -> "? super " + bound.toQualifiedTypeName();
        };
    }

    @Override
    public String toSourceWithoutAnnotation(){
        return switch(wildcardType){
            case WILD -> "?";
            case CONVARIANT -> "? extends " + bound.toSource("");
            case CONTRAVARIANT -> "? super " + bound.toSource("");
        };
    }

}
