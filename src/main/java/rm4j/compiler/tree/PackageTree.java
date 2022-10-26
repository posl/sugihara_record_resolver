package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;

/**
 * Represents the package declaration.
 *
 * @jls 7.3 Compilation Units
 * @jls 7.4 Package Declarations
 *
 * @author me
 */

@Tested(date = "2022/7/6", tester = "me", confidence = Status.CLEARLY_OK)
public record PackageTree(ArrayList<AnnotationTree> annotations, ExpressionNameTree name) implements Tree{

    static PackageTree parse(JavaTokenManager src) throws CompileException{
        var annotations = Tree.resolveAnnotations(src);
        src.skip(JavaTS.PACKAGE);
        ExpressionNameTree name = ExpressionNameTree.parse(src);
        src.skip(JavaTS.SEMICOLON);
        return new PackageTree(annotations, name);
    }

    PackageTree(){
        this(new ArrayList<>(), ExpressionNameTree.EMPTY);
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>(annotations);
        children.add(name);
        return children;
    }

}