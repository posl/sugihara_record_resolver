package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree node to stand in for a malformed expression.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */


@Deprecated
public record ErroneousTree(ArrayList<Tree> errorTrees)implements ExpressionTree{

    @Override
    public List<Tree> children(){
        return new ArrayList<>(errorTrees);
    }
    
}
