package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for a{@code case}in a{@code switch}statement or expression.
 *
 * For example:
 * <pre>
 *   case <em>expression</em> :
 *       <em>statements</em>
 *
 *   default :
 *       <em>statements</em>
 * </pre>
 *
 * @jls 14.11 The switch Statement
 *
 * @author me
 */
public record CaseTree(ArrayList<SwitchLabelTree> labels, ArrayList<StatementTree> statements, Tree body, CaseKind caseKind) implements Tree{
    
    /**
     * The syntactic form of this case:
     * <ul>
     *     <li>STATEMENT:{@code case <expression>: <statements>}</li>
     *     <li>RULE:{@code case <expression> -> <expression>/<statement>}</li>
     * </ul>
     *
     * @since 12
     */
    public enum CaseKind{
        /**
         * Case is in the form:{@code case <expression>: <statements>}.
         */
        STATEMENT,
        /**
         * Case is in the form:{@code case <expression> -> <expression>}.
         */
        RULE;
    }

    static CaseTree parse(JavaTokenManager src) throws CompileException{
        ArrayList<SwitchLabelTree> labels = new ArrayList<>();
        if(src.match(JavaTS.DEFAULT)){
            labels.add(DefaultCaseLabelTree.parse(src));
        }else{
            labels.add(CaseLabelTree.parse(src));
        }

        if(src.match(JavaTS.ARROW)){
            src.skip(JavaTS.ARROW);

            Tree body;

            if(src.match(JavaTS.LEFT_CURLY_BRACKET)){
                body = BlockTree.parse(src);
            }else if(src.match(JavaTS.THROW)){
                body = ThrowTree.parse(src);
            }else{
                body = ExpressionTree.parse(src);
                src.skip(JavaTS.SEMICOLON);
            }

            return new CaseTree(labels, null, body, CaseKind.RULE);
        }

        src.skip(JavaTS.COLON);
        while(true){
            if(src.match(JavaTS.DEFAULT)){
                labels.add(DefaultCaseLabelTree.parse(src));
            }else if(src.match(JavaTS.CASE)){
                labels.add(CaseLabelTree.parse(src));
            }else{
                break;
            }
            src.skip(JavaTS.COLON);
        }

        ArrayList<StatementTree> statements = new ArrayList<>();
        while(!src.match(JavaTS.DEFAULT) && !src.match(JavaTS.CASE) && !src.match(JavaTS.RIGHT_CURLY_BRACKET)){
            statements.add(StatementTree.resolveBlockStatement(src));
        }

        return new CaseTree(labels, statements, null, CaseKind.STATEMENT);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(labels);
        if(statements != null){
            children.addAll(statements);
        }
        if(body != null){
            children.add(body);
        }
        return children;
    }

}
