package rm4j.compiler.tree;

public interface TypeTree extends Tree{
    public static final TypeTree STRING = null;
    public static final TypeTree NULL = null;

    public String toQualifiedTypeName();
    default public String toSourceWithoutAnnotation(){
        return toSource("");
    }

}
