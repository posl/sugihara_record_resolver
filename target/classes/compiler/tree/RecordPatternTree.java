package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.ExpressionIdentifier;
import rm4j.compiler.resolution.Type;

/**
 * A tree node for a record pattern, which is provided as a preview feature in Java19.
 * 
 * For example:
 * <pre>
 *   <em>type</em> <em>identifier</em> < <em>typeArguments</em> >(<em>type</em> <em>identifier</em>, <em>type</em> <em>identifier</em>) <em>identifier</em>
 * </pre>
 * 
 * 
 * @author me
 */

public record RecordPatternTree(ModifiersTree modifiers, Tree type, ArrayList<PatternTree> componentPatterns, IdentifierTree identifier) implements PatternTree, ExpressionIdentifier{
    
    static RecordPatternTree parse(JavaTokenManager src) throws CompileException{
        var modifiers = ModifiersTree.parse(src);
        Tree type = NameTree.resolveTypeOrName(src);
        src.skip(JavaTS.LEFT_ROUND_BRACKET);
        ArrayList<PatternTree> componentPatterns = Tree.getList(PatternTree::parse, JavaTS.RIGHT_ROUND_BRACKET, src);
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);

        IdentifierTree identifier;
        if(src.match(IDENTIFIERS) && !src.match(JavaTS.WHEN)){
            identifier = IdentifierTree.parse(src);
        }else{
            identifier = null;
        }
        return new RecordPatternTree(modifiers, type, componentPatterns, identifier);
    }

    @Override
    public IdentifierTree simpleName(){
        return identifier;
    }

    @Override
    public Type inferedType(){
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        children.add(modifiers);
        children.add(type);
        children.addAll(componentPatterns);
        if(identifier != null){
            children.add(identifier);
        }
        return children;
    }

}
