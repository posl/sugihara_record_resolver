package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.ExpressionIdentifier;
import rm4j.compiler.resolution.Scope;
import rm4j.compiler.resolution.TreeTracker;
import rm4j.util.functions.CEConsumer;
import rm4j.util.functions.CEFunction;
import rm4j.util.functions.CEPredicate;

/**
 * Common interface for all nodes in an abstract syntax tree.
 *
 * @author me
 */

public interface Tree{

    static final HashSet<JavaTS> TYPE_IDENTIFIERS = new HashSet<>(Arrays.asList(
            JavaTS.IDENTIFIER, JavaTS.EXPORTS, JavaTS.MODULE, JavaTS.OPEN, JavaTS.OPENS, JavaTS.PROVIDES,
            JavaTS.REQUIRES, JavaTS.TO, JavaTS.TRANSITIVE,
            JavaTS.USES, JavaTS.WITH, JavaTS.WHEN));

    static final HashSet<JavaTS> IDENTIFIERS = new HashSet<>(Arrays.asList(
            JavaTS.IDENTIFIER, JavaTS.EXPORTS, JavaTS.MODULE, JavaTS.OPEN, JavaTS.OPENS, JavaTS.PERMITS,
            JavaTS.PROVIDES, JavaTS.RECORD, JavaTS.REQUIRES,
            JavaTS.SEALED, JavaTS.TO, JavaTS.TRANSITIVE, JavaTS.USES, JavaTS.VAR, JavaTS.WITH, JavaTS.YIELD,
            JavaTS.WHEN));

    static final HashSet<JavaTS> MODIFIERS = new HashSet<>(Arrays.asList(
            JavaTS.PUBLIC, JavaTS.PROTECTED, JavaTS.PRIVATE, JavaTS.FINAL, JavaTS.STATIC, JavaTS.ABSTRACT,
            JavaTS.DEFAULT,
            JavaTS.STRICTFP, JavaTS.SYNCHRONIZED, JavaTS.VOLATILE, JavaTS.TRANSIENT, JavaTS.NATIVE, JavaTS.SEALED,
            JavaTS.NON_SEALED));

    static final HashSet<JavaTS> LITERAL_TOKENS = new HashSet<>(Arrays.asList(
            JavaTS.INTEGER_LITERAL, JavaTS.FLOATING_POINT_LITERAL, JavaTS.TRUE, JavaTS.FALSE,
            JavaTS.CHARACTER_LITERAL, JavaTS.STRING_LITERAL, JavaTS.TEXT_BLOCK, JavaTS.NULL));

    static final HashSet<JavaTS> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            JavaTS.BYTE, JavaTS.SHORT, JavaTS.INT, JavaTS.LONG, JavaTS.CHAR,
            JavaTS.FLOAT, JavaTS.DOUBLE, JavaTS.BOOLEAN));

    public static ArrayList<AnnotationTree> resolveAnnotations(JavaTokenManager src) throws CompileException{
        var annotationList = new ArrayList<AnnotationTree>();
        while (src.match(JavaTS.AT_SIGN) && !src.match(JavaTS.AT_SIGN, JavaTS.INTERFACE)){
            annotationList.add(AnnotationTree.parse(src));
        }
        return annotationList;
    }

    enum DeclarationType{
        CLASS(JavaTS.CLASS, true, true, true, true, true),
        INTERFACE(JavaTS.INTERFACE, true, true, true, false, true),
        ENUM(JavaTS.ENUM, true, false, false, true, false),
        RECORD(JavaTS.RECORD, true, true, false, true, false),
        ANNOTATION_INTERFACE(JavaTS.INTERFACE, true, false, false, false, false){
            @Override
            JavaTS[] toTokens(){
                return new JavaTS[]{ JavaTS.AT_SIGN, JavaTS.INTERFACE };
            }

            @Override
            public String toString(){
                return "annotation interface";
            }
        },
        VARIABLE_DECLARATION(null, false, false, false, false, false),
        METHOD_DECLARATION(null, false, false, false, false, false),
        NOT_DECLARATION(null, false, false, false, false, false);

        final JavaTS token;
        final boolean hasTypeParameterClause;
        final boolean hasExtendsClause;
        final boolean hasImplementsClause;
        final boolean hasPermitsClause;
        final boolean isTypeDeclaration;

        private DeclarationType(JavaTS token, boolean... status){
            this.token = token;
            this.isTypeDeclaration = status[0];
            this.hasTypeParameterClause = status[1];
            this.hasExtendsClause = status[2];
            this.hasImplementsClause = status[3];
            this.hasPermitsClause = status[4];
        }

        static DeclarationType lookAheadDeclType(JavaTokenManager src) throws CompileException{
            var ptr = src.getPointer();
            LookAheadMode.MODIFIERS.skip(ptr);

            return switch (ptr.element().resolution){
                case CLASS -> CLASS;
                case INTERFACE -> INTERFACE;
                case ENUM -> ENUM;
                case AT_SIGN -> ANNOTATION_INTERFACE;
                case LESS_THAN -> METHOD_DECLARATION;
                default ->{
                    if (ptr.match(JavaTS.RECORD) && ptr.match(1, TYPE_IDENTIFIERS)){
                        yield RECORD;
                    }else if (ptr.match(JavaTS.YIELD) && !ptr.match(1, JavaTS.PERIOD)){
                        yield NOT_DECLARATION;
                    }
                    if (LookAheadMode.TYPE.skip(ptr)){
                        yield switch (ptr.element().resolution){
                            case LEFT_ROUND_BRACKET, LEFT_CURLY_BRACKET -> METHOD_DECLARATION;
                            default ->{
                                if (ptr.match(IDENTIFIERS)){
                                    ptr.next();
                                    if (ptr.match(JavaTS.LEFT_ROUND_BRACKET)){
                                        yield METHOD_DECLARATION;
                                    }else{
                                        yield VARIABLE_DECLARATION;
                                    }
                                }else{
                                    yield NOT_DECLARATION;
                                }
                            }
                        };
                    }
                    yield NOT_DECLARATION;
                }
            };
        }

        JavaTS[] toTokens(){
            return new JavaTS[]{ token };
        }

        @Override
        public String toString(){
            return token.key();
        }

    }

    static ArrayList<TypeTree> resolveTypeArguments(JavaTokenManager src) throws CompileException{
        ArrayList<TypeTree> typeArguments = new ArrayList<>();
        if (src.match(JavaTS.LESS_THAN, JavaTS.GREATER_THAN)){
            src.skip(JavaTS.LESS_THAN, JavaTS.GREATER_THAN);
            return ParameterizedTypeTree.DIAMOND;
        }else if (src.match(JavaTS.LESS_THAN)){
            src.skip(JavaTS.LESS_THAN);
            while (true){
                if (Tree.lookAhead(src, LookAheadMode.ANNOTATIONS) == JavaTS.QUESTION){
                    typeArguments.add(WildcardTree.parse(src));
                }else{
                    typeArguments.add(NameTree.resolveTypeOrName(src));
                }
                if (src.match(JavaTS.COMMA)){
                    src.skip(JavaTS.COMMA);
                }else{
                    break;
                }
            }
            src.formatGenericsClose();
            src.skip(JavaTS.GREATER_THAN);
        }
        return typeArguments;
    }

    public static <T extends Tree> ArrayList<T> getList(CEFunction<JavaTokenManager, T> getter, JavaTS rightBracket,
            JavaTokenManager src) throws CompileException{
        return getList(getter, JavaTS.COMMA, rightBracket, src);
    }

    public static <T extends Tree> ArrayList<T> getList(CEFunction<JavaTokenManager, T> getter, JavaTS separator,
            JavaTS rightBracket, JavaTokenManager src) throws CompileException{
        var list = new ArrayList<T>();
        if (src.match(separator)){
            src.skip(separator);
            return list;
        }
        while (!src.match(rightBracket)){
            list.add(getter.apply(src));
            if (src.match(separator)){
                src.skip(separator);
            }else{
                break;
            }
        }
        return list;
    }

    public static <T extends Tree> ArrayList<T> getListWithoutBracket(CEFunction<JavaTokenManager, T> getter, JavaTS separator,
            JavaTokenManager src) throws CompileException{
        var list = new ArrayList<T>();
        do{
            list.add(getter.apply(src));
            if (src.match(separator)){
                src.skip(separator);
            }else{
                break;
            }
        }while (true);
        return list;
    }

    public static String listToSource(List<? extends Tree> treeList, String separator, String indent){
        String s = "";
        for(int i = 0; i < treeList.size(); i++){
            s += treeList.get(i).toSource(indent) + ((i == treeList.size()-1)? "" : separator);
        }
        return s;
    }

    static boolean followsNameHeader(JavaTokenManager src) throws CompileException{
        return src.match(IDENTIFIERS) || src.match(PRIMITIVE_TYPES) || src.match(JavaTS.VOID) || src.match(JavaTS.VAR)
                || src.match(JavaTS.AT_SIGN);
    }

    static JavaTS lookAhead(JavaTokenManager src, LookAheadMode... modes) throws CompileException{
        var ptr = src.getPointer();
        for (LookAheadMode m : modes){
            if (!m.skip(ptr)){
                return JavaTS.END_OF_FILE;
            }
        }
        return ptr.element().resolution;
    }

    default ExpressionIdentifier getCorrespondingExpression(IdentifierTree id){
        return null;
    }

    default void setScope(TreeTracker tracker){
        tracker.trace.push(new Scope(this));
        resolve(tracker);
        tracker.trace.pop();
    }

    default public boolean literallyEquals(Tree t){
        if(getClass() == t.getClass()){
            List<Tree> c1 = children();
            List<Tree> c2 = t.children();
            if(c1.size() == c2.size()){
                for(int i = 0; i < c1.size(); i++){
                    if(!c1.get(i).literallyEquals(c2.get(i))){
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    default void resolve(TreeTracker tracker){
        throw new UnsupportedOperationException();
    }

    default String toSource(String indent){
        return toString();
    }

    default public List<Tree> children(){
        return new ArrayList<>(0);
    }

    public static void visit(Tree tree, CEConsumer<? super Tree> query) throws CompileException{
        List<Tree> children = new LinkedList<>();
        children.add(tree);
        do{
            final int size = children.size();
            for (int i = 0; i < size; i++){
                Tree child = children.get(0);
                children.addAll(child.children());
                query.accept(child);
                children.remove(0);
            }
        }while(!children.isEmpty());
    }

    public static List<Tree> extractChildren(Tree tree, CEPredicate<? super Tree> filter) throws CompileException{
        List<Tree> filtered = new ArrayList<>();
        List<Tree> children = new LinkedList<>();
        children.add(tree);
        do{
            final int size = children.size();
            for (int i = 0; i < size; i++){
                Tree child = children.get(0);
                if(filter.test(child)){
                    filtered.add(child);
                }else{
                    children.addAll(child.children());
                }
                children.remove(0);
            }
        }while(!children.isEmpty());
        return filtered;
    }

}
