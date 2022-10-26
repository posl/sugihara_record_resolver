package rm4j.compiler.tree;

import rm4j.compiler.core.CompileException;
import rm4j.compiler.core.JavaTS;
import rm4j.compiler.core.TokenList.TokenPointer;
import rm4j.test.Tested;
import rm4j.test.Tested.Status;


/**
 * An utility enum for lookAhead parsing.
 * @author me
 */

@Tested(date = "2022/7/8", tester = "me", confidence = Status.PROBABLY_OK)

public enum LookAheadMode{

    INSIDE_BRACKETS{

        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            int count = 0;
            LOOP: do{
                switch(ptr.element().resolution){
                    case END_OF_FILE ->{break LOOP;}
                    case LEFT_ROUND_BRACKET -> count++;
                    case RIGHT_ROUND_BRACKET -> count--;
                    default ->{}
                }
                ptr.next();
            }while(count > 0);
            if(ptr.element().resolution == JavaTS.END_OF_FILE){
                return false;
            }
            return true;
        }
        
    },

    QUALIFIED_NAME{

        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            if(ptr.match(Tree.IDENTIFIERS)){
                ptr.next();
            }else{
                return false;
            }
            while(ptr.match(JavaTS.PERIOD) && ptr.match(1, Tree.IDENTIFIERS)){
                ptr.next();
                ptr.next();
            }
            return true;
        }
        
    },

    ANNOTATIONS{

        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            while(ptr.match(JavaTS.AT_SIGN) && !ptr.match(1, JavaTS.INTERFACE)){
                ptr.next();
                if(!QUALIFIED_NAME.skip(ptr)){
                    throw new IllegalTokenException(ptr.element(), "type name");
                }
                if(ptr.match(JavaTS.LEFT_ROUND_BRACKET)){
                    INSIDE_BRACKETS.skip(ptr);
                }
            }
            return true;
        }

    },

    MODIFIERS{

        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            while(true){
                if(ptr.match(Tree.MODIFIERS) && !ptr.match(JavaTS.SEALED, JavaTS.PERIOD)){
                    ptr.next();
                }else if(ptr.match(JavaTS.AT_SIGN) && !ptr.match(1, JavaTS.INTERFACE)){
                    ANNOTATIONS.skip(ptr);
                }else{
                    break;
                }
            }
            return true;
        }

    },

    TYPE_ARGUMENTS{

        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            if(ptr.match(JavaTS.LESS_THAN)){
                int count = 0;
                JavaTS symbol;
                do{
                    switch(symbol = ptr.element().resolution){
                        case AT_SIGN -> ANNOTATIONS.skip(ptr);
                        case LESS_THAN -> count++;
                        case GREATER_THAN -> count--;
                        case BITWISE_SIGNED_RIGHT_SHIFT -> count -= 2;
                        case BITWISE_UNSIGNED_RIGHT_SHIFT -> count -= 3;
                        case QUESTION, SUPER, EXTENDS, COMMA, PERIOD, LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET->{}
                        default ->{
                            if(!Tree.TYPE_IDENTIFIERS.contains(symbol)
                                && !Tree.PRIMITIVE_TYPES.contains(symbol)){
                                return false;
                            }
                        }
                    }
                    ptr.next();
                }while(count > 0);
                if(count == -2){
                    ptr.setProvisionalToken(JavaTS.BITWISE_SIGNED_RIGHT_SHIFT);
                }else if(count == -1){
                    ptr.setProvisionalToken(JavaTS.GREATER_THAN);
                }
                return true;
            }
            return false;
        }

    },

    TYPE{
        
        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            TokenPointer subPtr;
            while(true){
                ANNOTATIONS.skip(ptr);
                if(ptr.match(Tree.IDENTIFIERS) || ptr.match(Tree.PRIMITIVE_TYPES) || ptr.match(JavaTS.VOID) || ptr.match(JavaTS.VAR)){
                    ptr.next();
                }else{
                    return false;
                }
                subPtr = ptr.clone();
                if(!TYPE_ARGUMENTS.skip(ptr)){
                    ptr.recover(subPtr);
                }
                if(ptr.match(JavaTS.PERIOD) && (ptr.match(1, JavaTS.AT_SIGN) || ptr.match(1, Tree.IDENTIFIERS))){
                    ptr.next();
                }else{
                    break;
                }
            }
            while(true){
                subPtr = ptr.clone();
                ANNOTATIONS.skip(ptr);
                if(ptr.match(JavaTS.LEFT_SQUARE_BRACKET, JavaTS.RIGHT_SQUARE_BRACKET)){
                    ptr.next();
                    ptr.next();
                }else if(ptr.match(JavaTS.ELLIPSIS)){
                    ptr.next();
                }else{
                    ptr.recover(subPtr);
                    break;
                }
            }
            return true;
        }

    },
    
    WILDCARD{
        @Override
        boolean skip(TokenPointer ptr) throws CompileException{
            if(ptr.match(JavaTS.QUESTION)){
                ptr.next();
            }else{
                return false;
            }
            if(ptr.match(JavaTS.EXTENDS) || ptr.match(JavaTS.SUPER)){
                ptr.next();
                if(!TYPE.skip(ptr)){
                    return false;
                }
            }
            return true;
        }
    };
    
    abstract boolean skip(TokenPointer ptr) throws CompileException;

}