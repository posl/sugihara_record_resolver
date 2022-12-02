package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.resolution.Accessor;

public record SuperTree(Accessor qualifier)implements ExpressionTree{

    static SuperTree parse(Accessor qualifier, JavaTokenManager src) throws CompileException{
        src.skip(JavaTS.SUPER);
        return new SuperTree(qualifier);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(1);
        children.add(qualifier);
        return children;
    }

}