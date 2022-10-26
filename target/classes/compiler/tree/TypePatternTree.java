package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.resolution.ExpressionIdentifier;
import rm4j.compiler.resolution.Type;

/**
 * A tree node used as the base class for the different kinds of
 * patterns.
 *
 * @since 16
 */
public record TypePatternTree(ModifiersTree modifiers, TypeTree type, IdentifierTree identifier) implements PatternTree, ExpressionIdentifier{

    static TypePatternTree parse(JavaTokenManager src) throws CompileException{
        var modifiers = ModifiersTree.parse(src);
        TypeTree type = NameTree.resolveTypeOrName(src);
        return new TypePatternTree(modifiers, type, IdentifierTree.parse(src));
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
        List<Tree> children = new ArrayList<>(3);
        children.add(modifiers);
        children.add(type);
        children.add(identifier);
        return children;
    }
    
}
