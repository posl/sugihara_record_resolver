package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.ParserException;
import rm4j.compiler.resolution.TreeTracker;
import rm4j.compiler.resolution.TypeIdentifier;

/**
 * A tree node for a class, interface, enum, record, or annotation
 * type declaration.
 *
 * For example:
 * 
 * <pre>
 *   <em>modifiers</em> class <em>simpleName</em> <em>typeParameters</em>
 *       implements <em>implementsClause</em>
 *       implements <em>implementsClause</em>
 *  {
 *       <em>members</em>
 *   }
 * </pre>
 *
 * @jls 8.1 Class Declarations
 * @jls 8.9 Enum Types
 * @jls 8.10 Record Types
 * @jls 9.1 Interface Declarations
 * @jls 9.6 Annotation Types
 *
 * @author me
 */

public record ClassTree(ModifiersTree modifiers, DeclarationType declType, IdentifierTree name,
        ArrayList<VariableTree> recordComponents,
        ArrayList<TypeParameterTree> typeParameters, TypeTree extendsClause, ArrayList<TypeTree> implementsClause,
        ArrayList<TypeTree> permitsClause, ArrayList<EnumConstantTree> enumConstants, ArrayList<Tree> members)
        implements StatementTree, TypeIdentifier{

    static ClassTree parse(DeclarationType declType, JavaTokenManager src) throws CompileException{
        var modifiers = ModifiersTree.parse(src);
        if (!declType.isTypeDeclaration){
            throw new ParserException("Executed \"ClassTree.parse()\", but there is no class declaration.");
        }
        src.skip(declType.toTokens());
        var name = IdentifierTree.parse(src);
        ArrayList<VariableTree> recordComponents = null;
        var typeParameters = new ArrayList<TypeParameterTree>();
        TypeTree extendsClause = null;
        var implementsClause = new ArrayList<TypeTree>();
        var permitsClause = new ArrayList<TypeTree>();
        ArrayList<EnumConstantTree> enumConstants = null;

        if (src.match(JavaTS.LESS_THAN)){
            src.skip(JavaTS.LESS_THAN);
            if (declType.hasTypeParameterClause){
                typeParameters = Tree.getList(TypeParameterTree::parse, JavaTS.GREATER_THAN, src);
            }else{
                throw new ParserException(
                        String.format("A %s declaration cannot have type parameters", declType.name()));
            }
            src.skip(JavaTS.GREATER_THAN);
        }
        if (src.match(JavaTS.LEFT_ROUND_BRACKET)){
            src.skip(JavaTS.LEFT_ROUND_BRACKET);
            if (declType == DeclarationType.RECORD){
                recordComponents = Tree.getList(VariableTree::resolveSingleDeclaration, JavaTS.RIGHT_ROUND_BRACKET,
                        src);
            }else{
                throw new ParserException(String.format("Only records have record header."));
            }
            src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        }else if(declType == DeclarationType.RECORD){
            throw new ParserException(String.format("Records must have record header."));
        }
        if (src.match(JavaTS.EXTENDS)){
            src.skip(JavaTS.EXTENDS);
            if (declType == DeclarationType.CLASS){
                extendsClause = NameTree.resolveNonArrayTypeOrName(src);
            }else if (declType == DeclarationType.INTERFACE){
                implementsClause = Tree.getListWithoutBracket(NameTree::resolveNonArrayTypeOrName, JavaTS.COMMA, src);
            }else{
                throw new ParserException(
                        String.format("A %s declaration cannot have implements clause.", declType.name()));
            }
        }
        if (src.match(JavaTS.IMPLEMENTS)){
            src.skip(JavaTS.IMPLEMENTS);
            if (declType.hasImplementsClause){
                implementsClause = Tree.getListWithoutBracket(NameTree::resolveNonArrayTypeOrName, JavaTS.COMMA, src);
            }else{
                throw new ParserException(
                        String.format("A %s declaration cannot have implements clause.", declType.name()));
            }
        }
        if (src.match(JavaTS.PERMITS)){
            src.skip(JavaTS.PERMITS);
            if (declType.hasPermitsClause){
                permitsClause = Tree.getListWithoutBracket(NameTree::resolveTypeOrName, JavaTS.COMMA, src);
            }else{
                throw new ParserException(
                        String.format("A %s declaration cannot have implements clause.", declType.name()));
            }
        }

        src.skip(JavaTS.LEFT_CURLY_BRACKET);
        ArrayList<Tree> members = new ArrayList<>();

        if (declType == DeclarationType.ENUM){
            enumConstants = resolveEnumConstants(src);
        }

        while (!src.match(JavaTS.RIGHT_CURLY_BRACKET)){
            members.add(resolveMember(declType, src));
        }
        src.skip(JavaTS.RIGHT_CURLY_BRACKET);

        return new ClassTree(modifiers, declType, name, recordComponents, typeParameters, extendsClause,
                implementsClause, permitsClause, enumConstants, members);
    }

    static ClassTree parse(TypeTree superType, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.LEFT_CURLY_BRACKET);

        ArrayList<Tree> members = new ArrayList<>();
        while (!src.match(JavaTS.RIGHT_CURLY_BRACKET)){
            members.add(resolveMember(DeclarationType.CLASS, src));
        }
        src.skip(JavaTS.RIGHT_CURLY_BRACKET);
        return new ClassTree(ModifiersTree.EMPTY, DeclarationType.CLASS, IdentifierTree.EMPTY, null,
                new ArrayList<>(), superType, new ArrayList<>(), new ArrayList<>(), null, members);
    }

    static ArrayList<EnumConstantTree> resolveEnumConstants(JavaTokenManager src) throws CompileException{
        var enumConstants = new ArrayList<EnumConstantTree>();
        while (IDENTIFIERS.contains(Tree.lookAhead(src, LookAheadMode.ANNOTATIONS))){
            enumConstants.add(EnumConstantTree.parse(src));
            if (src.match(JavaTS.COMMA)){
                src.skip(JavaTS.COMMA);
            }else{
                break;
            }
        }
        if (src.match(JavaTS.COMMA)){
            src.skip(JavaTS.COMMA);
        }
        if (src.match(JavaTS.SEMICOLON)){
            src.skip(JavaTS.SEMICOLON);
        }
        return enumConstants;
    }

    static Tree resolveMember(DeclarationType declType, JavaTokenManager src) throws CompileException{
        if (src.match(JavaTS.SEMICOLON)){
            return EmptyStatementTree.parse(src);
        }else if (src.match(JavaTS.LEFT_CURLY_BRACKET) || src.match(JavaTS.STATIC, JavaTS.LEFT_CURLY_BRACKET)){
            return BlockTree.parse(src);
        }else{
            DeclarationType typeDecl = DeclarationType.lookAheadDeclType(src);
            return switch (typeDecl){
                case VARIABLE_DECLARATION -> VariableTree.resolveDeclarationStatement(src);
                case METHOD_DECLARATION -> MethodTree.parse(src);
                case NOT_DECLARATION ->
                    throw new ParserException("Illegal statement in the class body: \"" + src.read() + "\"");
                default -> parse(typeDecl, src);
            };
        }
    }

    @Override
    public IdentifierTree simpleName(){
        return name;
    }

    @Override
    public void resolve(TreeTracker tracker){
        for (TypeParameterTree p : typeParameters){
            tracker.acceptLocalType(p);
        }
        if (recordComponents == null){
            for (VariableTree t : recordComponents){
                t.setScope(tracker);
            }
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        children.add(modifiers);
        children.add(name);
        if (recordComponents != null){
            children.addAll(recordComponents);
        }
        children.addAll(typeParameters);
        if (extendsClause != null){
            children.add(extendsClause);
        }
        children.addAll(implementsClause);
        children.addAll(permitsClause);
        if (enumConstants != null){
            children.addAll(enumConstants);
        }
        children.addAll(members);
        return children;
    }

}
