package rm4j.compiler.tree;

import java.util.ArrayList;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node used as the base class for the different kinds of
 * statements.
 *
 * @jls 14 Blocks and Statements
 *
 * @author me
 */

public interface StatementTree extends Tree{

    static StatementTree resolveBlockStatement(JavaTokenManager src) throws CompileException{
        DeclarationType declType = DeclarationType.lookAheadDeclType(src);
        if(declType.isTypeDeclaration){
            return ClassTree.parse(declType, src);
        }else if(declType == DeclarationType.VARIABLE_DECLARATION){
            return VariableTree.resolveDeclarationStatement(src);
        }else{
            return parse(src);
        }
    }

    static StatementTree parse(JavaTokenManager src) throws CompileException{
        if(src.match(IDENTIFIERS) && src.match(1, JavaTS.COLON)){
            return LabeledStatementTree.parse(src);
        }

        return switch(src.lookAhead().resolution){
            case IF -> IfTree.parse(src);
            case WHILE -> WhileLoopTree.parse(src);
            case FOR -> resolveForLoop(src);
            default -> resolveStatementWithoutTrailingSubstatement(src);
        };
    }

    static StatementTree resolveStatementWithoutTrailingSubstatement(JavaTokenManager src) throws CompileException{
        if(src.match(JavaTS.YIELD) &&
            (src.match(1, JavaTS.SIMPLE_ASSIGNMENT)
            || src.match(1, JavaTS.INCREMENT)
            || src.match(1, JavaTS.DECREMENT)
            || src.match(1, JavaTS.PERIOD)
            || src.match(1, JavaTS.DOUBLE_COLON)
            || src.match(1, JavaTS.LEFT_SQUARE_BRACKET))){
            return ExpressionStatementTree.parse(src);
        }

        return switch(src.lookAhead().resolution){
            case LEFT_CURLY_BRACKET -> BlockTree.parse(src);
            case SEMICOLON -> EmptyStatementTree.parse(src);
            case ASSERT -> AssertTree.parse(src);
            case SWITCH -> SwitchTree.parse(src);
            case DO -> DoWhileLoopTree.parse(src);
            case BREAK -> BreakTree.parse(src);
            case CONTINUE -> ContinueTree.parse(src);
            case RETURN  -> ReturnTree.parse(src);
            case SYNCHRONIZED -> SynchronizedTree.parse(src);
            case THROW -> ThrowTree.parse(src);
            case TRY -> TryTree.parse(src);
            case YIELD -> YieldTree.parse(src);
            default -> ExpressionStatementTree.parse(src);
        };
    }

    static StatementTree resolveForLoop(JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.FOR, JavaTS.LEFT_ROUND_BRACKET);
        
        ArrayList<StatementTree> init = new ArrayList<>();
        ExpressionTree expression = null;

        if(!src.match(JavaTS.SEMICOLON)){
            if(IDENTIFIERS.contains(Tree.lookAhead(src, LookAheadMode.MODIFIERS, LookAheadMode.TYPE))){
                VariableTree declaration = VariableTree.parse(src);
                if(src.match(JavaTS.COLON)){
                    src.skip(JavaTS.COLON);
                    expression = ExpressionTree.parse(src);
                    src.skip(JavaTS.RIGHT_ROUND_BRACKET);
                    return new EnhancedForLoopTree(declaration, expression, parse(src));
                }
                init.add(declaration);
            }else{
                init = Tree.getListWithoutBracket(ExpressionStatementTree::resolveStatementExpression, JavaTS.COMMA, src);
            }
        }
        src.skip(JavaTS.SEMICOLON);

        if(!src.match(JavaTS.SEMICOLON)){
            expression = ExpressionTree.parse(src);
        }
        src.skip(JavaTS.SEMICOLON);

        ArrayList<ExpressionStatementTree> update = new ArrayList<>();
        if(!src.match(JavaTS.RIGHT_ROUND_BRACKET)){
            update = Tree.getListWithoutBracket(ExpressionStatementTree::resolveStatementExpression, JavaTS.COMMA, src);
        }
        src.skip(JavaTS.RIGHT_ROUND_BRACKET);
        
        return new ForLoopTree(init, expression, update, parse(src));
    }
    
}
