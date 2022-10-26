package rm4j.compiler.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;

/**
 * A tree node for the modifiers, including annotations, for a declaration.
 *
 * For example:
 * <pre>
 *   <em>flags</em>
 *
 *   <em>flags</em> <em>annotations</em>
 * </pre>
 *
 * @jls 8.1.1 Class Modifiers
 * @jls 8.3.1 Field Modifiers
 * @jls 8.4.3 Method Modifiers
 * @jls 8.5.1 Static Member Type Declarations
 * @jls 8.8.3 Constructor Modifiers
 * @jls 9.1.1 Interface Modifiers
 * @jls 9.7 Annotations
 *
 * @author me
 * 
 */
public record ModifiersTree(ArrayList<Modifier> modifiers) implements Tree{

    static final ModifiersTree EMPTY = new ModifiersTree(new ArrayList<>());

    static ModifiersTree parse(JavaTokenManager src) throws CompileException{
        ArrayList<Modifier> modifiers = new ArrayList<>();
        OUTER: while(true){
            if(src.match(JavaTS.AT_SIGN) && !src.match(1, JavaTS.INTERFACE)){
                modifiers.add(AnnotationTree.parse(src));
            }else{
                for(ModifierKeyword m : ModifierKeyword.values()){
                    if(src.match(m.token) && !src.match(JavaTS.SEALED, JavaTS.PERIOD)){
                        src.skip(m.token);
                        modifiers.add(m);
                        continue OUTER;
                    }
                }
                break;
            }
        }
        return new ModifiersTree(modifiers);
    }


    public ArrayList<Modifier> getModifiers(){
        return modifiers;
    }

    boolean isEmpty(){
        return modifiers.isEmpty();
    }

    /**
     * Returns the flags in this modifiers tree.
     * @return the flags
     */

    HashSet<Modifier> getFlags(){
        //TODO fill body
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the annotations in this modifiers tree.
     * @return the annotations
     */
    ArrayList<AnnotationTree> getAnnotations(){
        var annotations = new ArrayList<AnnotationTree>();
        for(Modifier m : modifiers){
            if(m instanceof AnnotationTree a){
                annotations.add(a);
            }
        }
        return annotations;
    }

    public interface Modifier{}

    public enum ModifierKeyword implements Modifier{
        PUBLIC(JavaTS.PUBLIC),
        PROTECTED(JavaTS.PROTECTED),
        PRIVATE(JavaTS.PRIVATE),
        FINAL(JavaTS.FINAL),
        STATIC(JavaTS.STATIC),
        ABSTRACT(JavaTS.ABSTRACT),
        DEFAULT(JavaTS.DEFAULT),
        STRICTFP(JavaTS.STRICTFP),
        SYNCHRONIZED(JavaTS.SYNCHRONIZED),
        VOLATILE(JavaTS.VOLATILE),
        TRANSIENT(JavaTS.TRANSIENT),
        NATIVE(JavaTS.NATIVE),
        SEALED(JavaTS.SEALED),
        NON_SEALED(JavaTS.NON_SEALED);

        final JavaTS token;

        private ModifierKeyword(JavaTS token){
            this.token = token;
        }
    }

    @Override
    public List<Tree> children(){
        List<Tree> children = new ArrayList<>();
        if(this == EMPTY){
            return children;
        }
        for(Modifier m : modifiers){
            if(m instanceof Tree t){
                children.add(t);
            }
        }
        return children;
    }
}
