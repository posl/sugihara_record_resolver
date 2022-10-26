package rm4j.compiler.core;

/**
 * <p>
 * This is an enumeration of Java-syntax terminal symbols.
 * JavaSE 17 specification
 * https://docs.oracle.com/javase/specs/jls/se17/jls17.pdf
 * </p>
 */

public enum JavaTS{

    //Reserved keywords (52 tokens):
    ABSTRACT("abstract"),
    ASSERT("assert"),
    BOOLEAN("boolean"),
    BREAK("break"),
    BYTE("byte"),
    CASE("case"),
    CATCH("catch"),
    CHAR("char"),
    CLASS("class"),
    CONST("const"),
    CONTINUE("continue"),
    DEFAULT("default"),
    DO("do"),
    DOUBLE("double"),
    ELSE("else"),
    ENUM("enum"),
    EXTENDS("extends"),
    FINAL("final"),
    FINALLY("finally"),
    FLOAT("float"),
    FOR("for"),
    IF("if"),
    GOTO("goto"),
    IMPLEMENTS("implements"),
    IMPORT("import"),
    INSTANCEOF("instanceof"),
    INT("int"),
    INTERFACE("interface"),
    LONG("long"),
    NATIVE("native"),
    NEW("new"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    RETURN("return"),
    SHORT("short"),
    STATIC("static"),
    STRICTFP("strictfp"),
    SUPER("super"),
    SWITCH("switch"),
    SYNCHRONIZED("synchronized"),
    THIS("this"),
    THROW("throw"),
    THROWS("throws"),
    TRANSIENT("transient"),
    TRY("try"),
    VOID("void"),
    VOLATILE("volatile"),
    WHILE("while"),
    UNDERSCORE("_"),

    //Contextual keywords (16 tokens in Java18, 1 added("when") in Java19, 1 added("primitive") in Java2x):
    EXPORTS("exports"),
    MODULE("module"),
    NON_SEALED("non-sealed"),
    OPEN("open"),
    OPENS("opens"),
    PERMITS("permits"),
    PROVIDES("provides"),
    RECORD("record"),
    REQUIRES("requires"),
    SEALED("sealed"),
    TO("to"),
    TRANSITIVE("transitive"),
    USES("uses"),
    VAR("var"),
    WITH("with"),
    YIELD("yield"),
    WHEN("when"),

    //Literals
    INTEGER_LITERAL("#integer"),
    FLOATING_POINT_LITERAL("#floatingPoint"),
    TRUE("true"),
    FALSE("false"),
    CHARACTER_LITERAL("#character"),
    STRING_LITERAL("#string"),
    TEXT_BLOCK("#textBlock"),
    NULL("null"),

    //Separators (12 tokens):
    LEFT_ROUND_BRACKET("("),
    RIGHT_ROUND_BRACKET(")"),
    LEFT_CURLY_BRACKET("{"),
    RIGHT_CURLY_BRACKET("}"),
    LEFT_SQUARE_BRACKET("["),
    RIGHT_SQUARE_BRACKET("]"),
    SEMICOLON(";"),
    COMMA(","),
    PERIOD("."),
    ELLIPSIS("..."),
    AT_SIGN("@"),
    DOUBLE_COLON("::"),

    //Operators (38 tokens):
    SIMPLE_ASSIGNMENT("="),
    EQUAL_TO("=="),
    PLUS("+"),
    ASSIGNMENT_BY_SUM("+="),
    GREATER_THAN(">"),
    GRATER_THAN_OR_EQUAL_TO(">="),
    MINUS("-"),
    ASSIGNMENT_BY_DIFFERENCE("-="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL_TO("<="),
    ASTERISK("*"),
    ASSIGNMENT_BY_PRODUCT("*="),
    LOGICAL_NEGATION("!"),
    NOT_EQUAL_TO("!="),
    DIVISION("/"),
    ASSIGNMENT_BY_QUOTIENT("/="),
    BITWISE_NOT("~"),
    LOGICAL_CONDITIONAL_AND("&&"),
    AND("&"),
    ASSIGNMENT_BY_BITWISE_AND("&="),
    QUESTION("?"),
    LOGICAL_CONDITIONAL_OR("||"),
    VERTICAL_BAR("|"),
    ASSIGNMENT_BY_BITWISE_OR("|="),
    COLON(":"),
    INCREMENT("++"),
    BITWISE_AND_LOGICAL_XOR("^"),
    ASSIGNMENT_BY_BITWISE_XOR("^="),
    ARROW("->"),
    DECREMENT("--"),
    MODULO("%"),
    ASSIGNMENT_BY_REMINDER("%="),
    BITWISE_LEFT_SHIFT("<<"),
    ASSIGNMENT_BY_BITWISE_LEFT_SHIFT("<<="),
    BITWISE_SIGNED_RIGHT_SHIFT(">>"),
    ASSIGNMENT_BY_SIGNED_BITWISE_RIGHT_SHIFT(">>="),
    BITWISE_UNSIGNED_RIGHT_SHIFT(">>>"),
    ASSIGNMENT_BY_UNSIGNED_BITWISE_RIGHT_SHIFT(">>>="),

    //Others:
    IDENTIFIER("#identifier#"),
    END_OF_FILE("#eof");

    private String key;

    private JavaTS(String key){
        this.key = key;
    }

    public static JavaTS set(String key){
        for(JavaTS val : JavaTS.values()){
            if(val.key.equals(key)){
                return val;
            }
        }
        return IDENTIFIER;
    }

    public int id(){
        int i = 0;
        for(JavaTS ts : values()){
            if(ts == this){
                return i;
            }
            i++;
        }
        throw new UnsupportedOperationException();
    }

    public static JavaTS get(int id){
        return values()[id];
    }

    public String toString(){
        return key;
    }

    public String key(){
        return key;
    }
}
