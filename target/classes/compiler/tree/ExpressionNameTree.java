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
 * A tree node which is assumed as an expression name or method name contextually.
 * 
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record ExpressionNameTree(Accessor qualifier, IdentifierTree identifier) implements ExpressionTree, Accessor, TypeTree{

    public static final ExpressionNameTree EMPTY = new ExpressionNameTree();

    private ExpressionNameTree(){
        this(EMPTY, IdentifierTree.EMPTY);
    }

    static ExpressionNameTree parse(Accessor qualifier, JavaTokenManager src) throws CompileException{
        return new ExpressionNameTree(qualifier, IdentifierTree.parse(src));
    }

    static ExpressionNameTree parse(JavaTokenManager src) throws CompileException{
        var expr = new ExpressionNameTree(ExpressionNameTree.EMPTY, IdentifierTree.parse(src));
        while(src.match(JavaTS.PERIOD) && src.match(1, IDENTIFIERS)){
            src.skip(JavaTS.PERIOD);
            expr = parse(expr, src);
        }
        return expr;
    }

    static ExpressionTree convertToExpression(Tree t) throws CompileException{
        if(t == EMPTY){
            return null;
        }else if(t instanceof ExpressionTree e){
            return e;
        }else if(t instanceof NameTree n){
            return new ExpressionNameTree(n.qualifier(), n.identifier());
        }else{
            throw new ParserException(String.format("Tree \"%s\" cannot be converted to an expression.", t.toString()));
        }
    }

    public String toSource(String indent){
        return (qualifier == ExpressionNameTree.EMPTY)?
                identifier.name() : qualifier.toSource(indent)+"."+identifier.name(); 
    }

    @Override
    public boolean literallyEquals(Tree t){
        if(t.getClass() == NameTree.class || t.getClass() == ExpressionNameTree.class){
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
    
    @Override
    public List<Tree> children(){
        if(this == EMPTY){
            return new ArrayList<>(0);
        }
        List<Tree> children = new ArrayList<>(2);
        children.add(qualifier);
        children.add(identifier);
        return children;
    }

    @Override
    public String toQualifiedTypeName(){
        if(qualifier instanceof TypeTree typeName){
            return ((qualifier == ExpressionNameTree.EMPTY)?
                "" : typeName.toQualifiedTypeName()+".") + identifier.name(); 
        }else{
            return "$NOT_TYPE";
        }
    }

}
