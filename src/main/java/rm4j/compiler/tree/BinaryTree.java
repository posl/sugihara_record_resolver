package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * A tree node for a binary expression.
 * Use{@link #getKind getKind}to determine the kind of operator.
 *
 * For example:
 * <pre>
 *   <em>leftOperand</em> <em>operator</em> <em>rightOperand</em>
 * </pre>
 *
 * @jls 15.17 Multiplicative Operators
 * @jls 15.18 Additive Operators
 * @jls 15.19 Shift Operators
 * @jls 15.20 Relational Operators
 * @jls 15.21 Equality Operators
 * @jls 15.22 Bitwise and Logical Operators
 * @jls 15.23 Conditional-And Operator{@code &&}
 * @jls 15.24 Conditional-Or Operator{@code ||}
 *
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)
public record BinaryTree(ExpressionTree leftOperand, BinaryOperator operatorKind, JavaTS operatorToken, ExpressionTree rightOperand) implements ExpressionTree{

    enum BinaryOperator{
        UNARY(new JavaTS[]{}, null){
            
            @Override
            ExpressionTree getBinaryExpression(JavaTokenManager src) throws CompileException{
                return UnaryTree.UnaryOperator.PREFIX.getUnaryExpression(src);
            }

        },
        MULTIPLICATIVE(new JavaTS[]{JavaTS.ASTERISK, JavaTS.DIVISION, JavaTS.MODULO}, UNARY),
        ADDITIVE(new JavaTS[]{JavaTS.PLUS, JavaTS.MINUS}, MULTIPLICATIVE),
        SHIFT(new JavaTS[]{JavaTS.BITWISE_LEFT_SHIFT, JavaTS.BITWISE_SIGNED_RIGHT_SHIFT, JavaTS.BITWISE_UNSIGNED_RIGHT_SHIFT}, ADDITIVE),
        RELATIONAL(new JavaTS[]{JavaTS.LESS_THAN, JavaTS.GREATER_THAN, JavaTS.LESS_THAN_OR_EQUAL_TO, JavaTS.GRATER_THAN_OR_EQUAL_TO}, SHIFT){

            @Override
            ExpressionTree getBinaryExpression(JavaTokenManager src) throws CompileException{
                ExpressionTree left = super.getBinaryExpression(src);
                if(src.match(JavaTS.INSTANCEOF)){
                    return InstanceOfTree.parse(left, src);
                }
                return left;
            }

        },
        EQUALITY(new JavaTS[]{JavaTS.EQUAL_TO, JavaTS.NOT_EQUAL_TO}, RELATIONAL),
        AND(new JavaTS[]{JavaTS.AND}, EQUALITY),
        EXCLUSIVE_OR(new JavaTS[]{JavaTS.BITWISE_AND_LOGICAL_XOR}, AND),
        INCLUSIVE_OR(new JavaTS[]{JavaTS.VERTICAL_BAR},EXCLUSIVE_OR),
        CONDITIONAL_AND(new JavaTS[]{JavaTS.LOGICAL_CONDITIONAL_AND}, INCLUSIVE_OR),
        CONDITIONAL_OR(new JavaTS[]{JavaTS.LOGICAL_CONDITIONAL_OR}, CONDITIONAL_AND);

        protected final JavaTS[] tokens;
        protected final BinaryOperator prior;

        private BinaryOperator(JavaTS[] tokens, BinaryOperator prior){
            this.tokens = tokens;
            this.prior = prior;
        }

        ExpressionTree getBinaryExpression(JavaTokenManager src) throws CompileException{
            ExpressionTree left = prior.getBinaryExpression(src);
            OUTER: while(true){
                for(JavaTS s : tokens){
                    if(!src.match(s)){
                        continue;
                    }
                    src.skip(s);
                    left = new BinaryTree(left, this, s, prior.getBinaryExpression(src));
                    continue OUTER;
                }
                break;
            }
            return left;
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(2);
        children.add(leftOperand);
        children.add(rightOperand);
        return children;
    }
    
}
