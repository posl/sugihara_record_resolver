package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.ParserException;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for postfix and unary expressions.
 * Use{@link #getKind getKind}to determine the kind of operator.
 *
 * For example:
 * <pre>
 *   <em>operator</em> <em>expression</em>
 *
 *   <em>expression</em> <em>operator</em>
 * </pre>
 *
 * @jls 15.14 Postfix Expressions
 * @jls 15.15 Unary Operators
 *
 * @author me
 */


@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record UnaryTree(UnaryOperator operatorType, JavaTS operatorToken, ExpressionTree expression) implements ExpressionTree{

    enum UnaryOperator{
        PREFIX(new JavaTS[]{JavaTS.INCREMENT, JavaTS.DECREMENT, JavaTS.PLUS, JavaTS.MINUS}){

            @Override
            public ExpressionTree getUnaryExpression(JavaTokenManager src) throws CompileException{
                for(JavaTS s : tokens){
                    if(!src.match(s)){
                        continue;
                    }
                    src.skip(s);
                    return new UnaryTree(this, s, getUnaryExpression(src));
                }
                return NOT_PLUS_MINUS.getUnaryExpression(src);
            }

        },

        NOT_PLUS_MINUS(new JavaTS[]{JavaTS.BITWISE_NOT, JavaTS.LOGICAL_NEGATION}){

            @Override
            public ExpressionTree getUnaryExpression(JavaTokenManager src) throws CompileException{
                for(JavaTS s : tokens){
                    if(!src.match(s)){
                        continue;
                    }
                    src.skip(s);
                    return new UnaryTree(this, s, PREFIX.getUnaryExpression(src));
                }
                if(src.match(JavaTS.LEFT_ROUND_BRACKET)){
                    return getTypeCastOrExpression(src);
                }else if(src.match(JavaTS.SWITCH)){
                    return SwitchExpressionTree.parse(src);
                }
                return POSTFIX.getUnaryExpression(src);
            }

        },

        POSTFIX(new JavaTS[]{JavaTS.INCREMENT, JavaTS.DECREMENT}){
            @Override
            public ExpressionTree getUnaryExpression(JavaTokenManager src) throws CompileException{
                return getPostfixExpression(ExpressionTree.resolvePrimary(src), src);
            }

        };

        protected final JavaTS[] tokens;
        private UnaryOperator(JavaTS[] tokens){
            this.tokens = tokens;
        }

        static ExpressionTree getTypeCastOrExpression(JavaTokenManager src) throws CompileException{
            Tree type;
            if(src.match(1, PRIMITIVE_TYPES) && src.match(2, JavaTS.RIGHT_ROUND_BRACKET)){
                src.skip(JavaTS.LEFT_ROUND_BRACKET);
                type = PrimitiveTypeTree.parse(src);
                src.skip(JavaTS.RIGHT_ROUND_BRACKET);
                return new TypeCastTree(type, PREFIX.getUnaryExpression(src));
            }else{
                JavaTS symbol;
                switch(symbol = Tree.lookAhead(src, LookAheadMode.INSIDE_BRACKETS)){
                    case LOGICAL_NEGATION, BITWISE_NOT, LEFT_ROUND_BRACKET, SWITCH, 
                        NEW, SUPER, THIS, AT_SIGN ->{break;}
                    default ->{
                        if(IDENTIFIERS.contains(symbol) || LITERAL_TOKENS.contains(symbol) || PRIMITIVE_TYPES.contains(symbol)){
                            break;
                        }
                        return POSTFIX.getUnaryExpression(src);
                    }
                }
                src.skip(JavaTS.LEFT_ROUND_BRACKET);
                type = switch(Tree.lookAhead(src, LookAheadMode.TYPE)){
                    case RIGHT_ROUND_BRACKET -> NameTree.resolveTypeOrName(src);
                    case AND -> IntersectionTypeTree.parse(src);
                    default -> throw new ParserException("Illegal expression in brackets, expected type name.");
                };
                src.skip(JavaTS.RIGHT_ROUND_BRACKET);                    
                if(ExpressionTree.followsLambdaExpression(src)){
                    return new TypeCastTree(type, LambdaExpressionTree.parse(src));
                }
                return new TypeCastTree(type, NOT_PLUS_MINUS.getUnaryExpression(src));
            }
        }
        
        public abstract ExpressionTree getUnaryExpression(JavaTokenManager src) throws CompileException;

        public ExpressionTree getPostfixExpression(ExpressionTree expr, JavaTokenManager src) throws CompileException{
            OUTER : while(true){
                for(JavaTS s : POSTFIX.tokens){
                    if(!src.match(s)){
                        continue;
                    }
                    src.skip(s);
                    expr = new UnaryTree(POSTFIX, s, expr);
                    continue OUTER;
                }
                break;
            }
            return expr;
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(expression);
        return children;
    }
    
}
