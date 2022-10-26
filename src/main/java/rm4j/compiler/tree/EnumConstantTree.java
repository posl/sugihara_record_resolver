package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.ExpressionIdentifier;
import rm4j.compiler.resolution.Type;

public record EnumConstantTree(ArrayList<AnnotationTree> annotations, IdentifierTree name, ArrayList<ExpressionTree> arguments, ArrayList<Tree> members) implements Tree, ExpressionIdentifier{

    static EnumConstantTree parse(JavaTokenManager src) throws CompileException{
        var annotations = Tree.resolveAnnotations(src);
        IdentifierTree name = IdentifierTree.parse(src);
        ArrayList<ExpressionTree> arguments = null;
        if(src.match(JavaTS.LEFT_ROUND_BRACKET)){
            arguments = ExpressionTree.resolveArguments(src);
        }
        ArrayList<Tree> members = null;
        if(src.match(JavaTS.LEFT_CURLY_BRACKET)){
            src.skip(JavaTS.LEFT_CURLY_BRACKET);
            members = new ArrayList<>();
            while(!src.match(JavaTS.RIGHT_CURLY_BRACKET)){
                members.add(ClassTree.resolveMember(DeclarationType.CLASS, src));
            }
            src.skip(JavaTS.RIGHT_CURLY_BRACKET);
        }
        return new EnumConstantTree(annotations, name, arguments, members);
    }

    @Override
    public IdentifierTree simpleName(){
        return name;
    }

    @Override
    public Type inferedType(){
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(annotations);
        children.add(name);
        if(arguments != null){
            children.addAll(arguments);
        }
        if(members != null){
            children.addAll(members);
        }
        return children;
    }
    
}