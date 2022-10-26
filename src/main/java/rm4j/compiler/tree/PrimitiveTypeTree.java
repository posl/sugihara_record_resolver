package rm4j.compiler.tree;

import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.resolution.PrimitiveType;

/**
 * A tree node for a primitive type.
 *
 * For example:
 * <pre>
 *   <em>primitiveTypeKind</em>
 * </pre>
 *
 * @jls 4.2 Primitive Types and Values
 * 
 * @author me
 */

public record PrimitiveTypeTree(PrimitiveType primitiveType) implements TypeTree{

    static PrimitiveTypeTree parse(JavaTokenManager src) throws CompileException{
        return new PrimitiveTypeTree(PrimitiveType.get(src.read()));
    }

}
