package rm4j.compiler.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import rm4j.compiler.tokens.*;

/**
 * This is a lexer of JavaSE 17.
 * 
 * @author me
 */

public class JavaLexer{
    private static final Set<Character> EMPTY = new HashSet<>();
    private static final Set<Character> LINE_TERMINATE_CHARACTERS = new HashSet<>(Arrays.asList('\n', '\r'));
    private static final Set<Character> WHITE_SPACE_CHARACTERS = new HashSet<>(Arrays.asList(' ', '\t', '\f'));
    private static final Set<Character> WHITE_SPACE_OR_LINE_TERMINATE_CHARACTERS = new HashSet<>(
            Arrays.asList(' ', '\t', '\f', '\n', '\r'));
    private static final Set<Character> HEX_DIGITS = new HashSet<>(
            Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C',
                    'D', 'E', 'F'));
    private static final Set<Character> NON_ZERO_DIGITS = new HashSet<>(
            Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9'));
    private static final Set<Character> DIGITS = new HashSet<>(
            Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
    private static final Set<Character> OCTAL_DIGITS = new HashSet<>(
            Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7'));
    private static final Set<Character> ZERO_TO_THREE = new HashSet<>(Arrays.asList('0', '1', '2', '3'));
    private static final Set<Character> BINARY_DIGITS = new HashSet<>(Arrays.asList('0', '1'));

    private static final Set<Character> EXPONENT_INDICATOR = new HashSet<>(Arrays.asList('e', 'E'));
    private static final Set<Character> BINARY_EXPONENT_INDICATOR = new HashSet<>(Arrays.asList('p', 'P'));
    private static final Set<Character> SIGNS = new HashSet<>(Arrays.asList('+', '-'));
    private static final Set<Character> INTEGER_TYPE_SUFFIXES = new HashSet<>(Arrays.asList('l', 'L'));
    private static final Set<Character> FLOAT_SUFFIXES = new HashSet<>(Arrays.asList('f', 'F'));
    private static final Set<Character> DOUBLE_SUFFIXES = new HashSet<>(Arrays.asList('d', 'D'));

    private static final Set<Character> ESCAPE_SEQUENCE_MARKERS = new HashSet<>(
            Arrays.asList('b', 's', 't', 'n', 'f', 'r', '\"', '\'', '\\', '\n', '\r'));
    private static final Set<Character> SEPARATOR_INTRODUCTION_CHARACTERS = new HashSet<>(
            Arrays.asList('(', ')', '{', '}', '[', ']', ';', ',', '@'));
    private static final Set<Character> OPERATOR_INTRODUCTION_CHARACTERS = new HashSet<>(
            Arrays.asList('=', '>', '<', '!', '~', '?', '+', '-', '*', '&', '|', '^', '%'));

    private static final Set<String> SEPARATORS = new HashSet<>(
            Arrays.asList("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "...", "@", "::"));
    private static final Set<String> OPERATORS = new HashSet<>(
            Arrays.asList("=", ">", "<", "!", "~", "?", ":", "->",
                    "==", ">=", "<=", "!=", "&&", "||", "++", "--",
                    "+", "-", "*", "/", "&", "|", "^", "%", "<<", ">>", ">>>",
                    "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>="));
    private static final Set<String> BOOLEAN_LITERALS = new HashSet<>(Arrays.asList("false", "true"));
    private static final Set<String> NULL_LITERAL = new HashSet<>(Arrays.asList("null"));
    private static final Set<String> KEYWORDS = new HashSet<>(
            Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                    "for", "if", "goto", "implements", "import", "instanceof", "int", "interface", "long", "native",
                    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                    "super",
                    "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile",
                    "while",
                    "_"));
    private static final Set<String> CONTEXTUAL_KEYWORDS = new HashSet<>(
            Arrays.asList("open", "module", "requires", "transitive", "exports", "opens", "to", "uses",
                    "provides", "with", "var", "permits", "sealed", "non-sealed", "record", "yield"));

    private static Literal getNumericLiteral(String prefix, CharList source, ReferenceProducer recorder)
            throws CompileException, IndexOutOfBoundsException{
        Base base = Base.set(prefix);
        String integerPart = prefix + getDigits(source, base.digitChars);
        if (source.refer() != null && source.refer() == '.'){
            return readAfterPoint(source, integerPart + source.get(), base, recorder);
        }else{
            return readAfterPoint(source, integerPart, base, recorder);
        }
    }

    private static Literal readAfterPoint(CharList source, String s, Base base, ReferenceProducer recorder)
            throws CompileException, IndexOutOfBoundsException{
        boolean isInteger = !lastCharacterOf(s).equals(".");
        s += getDigits(source, base.digitChars);
        // exponent part resolving
        if (base.exponentIndicator.contains(source.refer())){
            isInteger = false;
            s += source.get();
            if (SIGNS.contains(source.refer())){
                s += source.get();
            }
            s += getDigits(source, DIGITS);
        }

        try{

            if (FLOAT_SUFFIXES.contains(source.refer()) || DOUBLE_SUFFIXES.contains(source.refer())){
                s += source.get();
                return new FloatingPointLiteral(s, recorder.get(source), base.radix(s, false));
            }

            if (isInteger){
                if (INTEGER_TYPE_SUFFIXES.contains(source.refer())){
                    s += source.get();
                }
                return new IntegerLiteral(s, recorder.get(source), base.radix(s, true));
            }
            return new FloatingPointLiteral(s, recorder.get(source), base.radix(s, false));

        }catch (NumberFormatException e){
            throw new LexerException("Invalid number literal.");
        }
    }

    private static String getDigits(CharList source, Set<Character> digits)
            throws CompileException, IndexOutOfBoundsException{
        String s = source.readForward(c -> digits.contains(c) || c == '_');
        if (lastCharacterOf(s).equals("_")){
            throw new LexerException("Underscores have to be located within digits.");
        }
        return s;
    }

    private static String lastCharacterOf(String s){
        if (s.length() == 0){
            return "";
        }else{
            return s.substring(s.length() - 1);
        }
    }

    private static String getLineTerminator(CharList source){
        if (source.refer() != null && source.refer() == '\r'){
            String s = String.valueOf(source.get());
            if (source.refer() != null && source.refer() == '\n'){
                return s + source.get();
            }
            return s;
        }else if (source.refer() != null && source.refer() == '\n'){
            return String.valueOf(source.get());
        }
        return "";
    }

    private static String getCharacter(CharList source, char... illegals)
            throws CompileException, IndexOutOfBoundsException{
        char c = source.get();
        if (c == '\\'){
            return c + getEscapeSequence(source);
        }
        for (char illegal : illegals){
            if (c == illegal){
                throw new LexerException("Illegal literal.");
            }
        }
        return String.valueOf(c);
    }

    private static String getEscapeSequence(CharList source) throws CompileException, IndexOutOfBoundsException{
        char c = source.get();
        if (ESCAPE_SEQUENCE_MARKERS.contains(c)){
            return String.valueOf(c);
        }else if (OCTAL_DIGITS.contains(c)){
            String s = String.valueOf(c);
            if (OCTAL_DIGITS.contains(source.refer())){
                s += source.get();
                if (ZERO_TO_THREE.contains(c) && OCTAL_DIGITS.contains(source.refer())){
                    s += source.get();
                }
            }
            return s;
        }
        throw new LexerException("Invalid character literal.");
    }

    /**
     * Gets CharList from the source file.
     * All Unicode escapes are translated to corresponding Unicode characters.
     * 
     * @param file source file
     * @return CharList expression
     */

    private CharList translateUnicode(File file) throws CompileException, IndexOutOfBoundsException{
        enum Status{
            BACK_SLASH, DEFAULT
        }
        var charList = new CharList();
        Status status = Status.DEFAULT;

        try (var reader = new FileReader(file)){
            int ch;
            int bscount = 0;
            while ((ch = reader.read()) != -1){
                if (ch == '\\'){
                    status = Status.BACK_SLASH;
                    bscount++;
                }else{
                    if (ch == 'u' && status == Status.BACK_SLASH
                            && bscount % 2 == 1){
                        charList.remove(1);
                        ch = getUnicode(reader);
                    }
                    status = Status.DEFAULT;
                    bscount = 0;
                }
                charList.add((char) ch);
            }
        }catch (IOException e){
            e.printStackTrace();
            throw new LexerException("The lexer reported an error in the resolution of " + file.toString() + ".");
        }
        charList.reset();
        return charList;
    }

    /**
     * Gets a Unicode character from FileReader.
     * The appearence of odd number of '\'(backslash) followed by character 'u'
     * confirms the appearence of Unicode escape.
     */

    private int getUnicode(FileReader reader) throws IOException, LexerException{
        int ch;
        String hex = "0x";

        try{
            do{
                ch = reader.read();
            }while (ch == 'u');
            hex += (char) ch;
            for (int i = 1; i < 4; i++){
                ch = reader.read();
                hex += (char) ch;
            }
        }catch (IndexOutOfBoundsException e){
            throw new LexerException("Found invalid Unicode.");
        }

        try{
            return Integer.decode(hex);
        }catch (NumberFormatException e){
            throw new LexerException("Found invalid Unicode.");
        }
    }

    /**
     * Gets TokenList from CharList.
     */

    private TokenList tokenize(File file) throws CompileException{
        var source = translateUnicode(file);
        var tokens = new TokenList(file);
        var recorder = new ReferenceProducer();
        try{
            while (source.hasNext()){
                var element = Terminal.set(source.refer()).getInputElement(source, recorder);
                if (element instanceof Identifier i && i.resolution == JavaTS.SEALED){
                    if (tokens.applyTests(
                            e -> e instanceof Operator o && o.resolution == JavaTS.MINUS,
                            e -> e instanceof Identifier id && id.text.equals("non"))){
                        tokens.remove(2);
                        recorder.rollBack(3);
                        tokens.add(new Identifier("non-sealed", recorder.get(tokens.refer().ref.line, source)));
                        continue;
                    }
                }
                tokens.add(element);
            }
        }catch (IndexOutOfBoundsException e){
            throw new LexerException("Source ended with an invalid token.\nat " + tokens.getCurrentReference() + ".");
        }catch (LexerException e){
            throw new LexerException(e.toString() + "\nat " + tokens.getCurrentReference() + ".");
        }
        tokens.add(Token.EOF);
        tokens.reset();
        return tokens;
    }

    public TokenList run(File file) throws CompileException{
        return tokenize(file);
    }

    class ReferenceProducer{
        private int pos = 0;

        public Reference get(CharList source){
            return new Reference(source.line(), ++pos);
        }

        public Reference get(int line, CharList source){
            Reference reference = new Reference(line, ++pos);
            if (line != source.line()){
                pos = 0;
            }
            return reference;
        }

        public void rollBack(int i) throws UnsupportedOperationException{
            if (i > pos){
                throw new UnsupportedOperationException();
            }
            pos -= i;
        }
    }

    /**
     * This enum supports reading numeric literals.
     * The first argument of the constants specifies what characters are allowed
     * while reading literals.
     */

    enum Base{
        BINARY(BINARY_DIGITS, EMPTY, 2, "0b", "0B"){

            @Override
            public int radix(String s, boolean isInteger) throws NumberFormatException{
                if (!isInteger){
                    throw new NumberFormatException("Invalid number literal.");
                }
                return 2;
            }

        },

        DECIMAL_OR_OCTAL(DIGITS, EXPONENT_INDICATOR, 10, "", "0"){

            @Override
            public int radix(String s, boolean isInteger) throws NumberFormatException{
                if (isInteger){
                    if (lastCharacterOf(s).equals("l") || lastCharacterOf(s).equals("L")){
                        s = s.substring(0, s.length() - 1);
                    }
                    if (s.length() > 1 && s.startsWith("0")){
                        return 8;
                    }
                }
                return 10;
            }

        },
        HEXADECIMAL(HEX_DIGITS, BINARY_EXPONENT_INDICATOR, 16, "0x", "0X");

        Set<Character> digitChars;
        Set<Character> exponentIndicator;
        int radix;
        String[] prefixes;

        private Base(Set<Character> digitChars, Set<Character> exponentIndicator, int radix, String... prefixes){
            this.digitChars = digitChars;
            this.exponentIndicator = exponentIndicator;
            this.radix = radix;
            this.prefixes = prefixes;
        }

        public static Base set(String prefix) throws CompileException, IndexOutOfBoundsException{
            for (Base base : Base.values()){
                for (String pf : base.prefixes){
                    if (pf.equals(prefix))
                        return base;
                }
            }
            throw new LexerException("Illegal prefix of numeric literal.");
        }

        public int radix(String s, boolean isInteger) throws NumberFormatException{
            return radix;
        }
    }

    enum Terminal{

        WHITE_SPACE_INTRO(c -> WHITE_SPACE_OR_LINE_TERMINATE_CHARACTERS.contains(c)){

            @Override
            public WhiteSpace getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                int line = source.line();
                var ws = new WhiteSpace(source.readForward(WHITE_SPACE_OR_LINE_TERMINATE_CHARACTERS),
                        recorder.get(line, source));
                return ws;
            }

        },

        SLASH(c -> c == '/'){

            @Override
            public InputElement getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = String.valueOf(source.get());
                int line = source.line();
                return switch (source.refer()){
                    case '*' ->{
                        source.get();
                        s = "";
                        do{
                            s += source.get();
                        }while (!s.endsWith("*/"));
                        yield new Comment("/*" + s, recorder.get(line, source));
                    }

                    case '/' ->{
                        s += source.readForward(c -> !LINE_TERMINATE_CHARACTERS.contains(c));
                        yield new Comment(s, recorder.get(source));
                    }

                    case '=' ->{
                        s += source.get();
                        yield new Operator(s, recorder.get(source));
                    }

                    default -> new Operator(s, recorder.get(source));
                };
            }

        },

        WORD_INTRO(c -> Character.isJavaIdentifierStart(c)){

            @Override
            public Token getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = "";
                s += source.readForward(Character::isJavaIdentifierPart);
                if (KEYWORDS.contains(s)){
                    return new Keyword(s, recorder.get(source));
                }
                return switch (s){
                    case "true", "false" -> new BooleanLiteral(s.equals("true"), recorder.get(source));
                    case "null" -> new NullLiteral(recorder.get(source));
                    case "instanceof" -> new Operator(s, recorder.get(source));
                    default -> new Identifier(s, recorder.get(source));
                };
            }

        },

        DECIMAL_NUMBER_INTRO(c -> NON_ZERO_DIGITS.contains(c)){

            @Override
            public Literal getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                return getNumericLiteral("", source, recorder);
            }

        },

        ZERO(c -> c == '0'){

            @Override
            public Literal getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = String.valueOf(source.get());
                return switch (source.refer()){
                    case 'x', 'X', 'b', 'B' -> getNumericLiteral(s + source.get(), source, recorder);
                    default -> getNumericLiteral(s, source, recorder);
                };
            }

        },

        PERIOD(c -> c == '.'){

            @Override
            public Token getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = String.valueOf(source.get());
                if (DIGITS.contains(source.refer())){
                    return readAfterPoint(source, ".", Base.DECIMAL_OR_OCTAL, recorder);
                }else if (source.refer() != null && source.refer() == '.'){
                    s += source.get();
                    if (source.refer() != null && source.refer() == '.'){
                        s += source.get();
                        return new Separator(s, recorder.get(source));
                    }else{
                        throw new LexerException("Invalid token \".\".");
                    }
                }else{
                    return new Operator(s, recorder.get(source));
                }
            }

        },

        COLON(c -> c == ':'){

            @Override
            public Token getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = String.valueOf(source.get());
                if (source.refer() != null && source.refer() == ':'){
                    return new Operator(s + source.get(), recorder.get(source));
                }else{
                    return new Separator(s, recorder.get(source));
                }
            }

        },

        CHARACTER_LITERAL_INTRO(c -> c == '\''){

            @Override
            public CharacterLiteral getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                source.get();
                String s = getCharacter(source, '\r', '\n', '\'');
                if (source.get() != '\''){
                    throw new LexerException("Invalid character literal.");
                }
                return new CharacterLiteral(s, recorder.get(source));
            }

        },

        STRING_LITERAL_INTRO(c -> c == '\"'){

            @Override
            public Literal getInputElement(CharList source, ReferenceProducer recorder)
                    throws CompileException, IndexOutOfBoundsException{
                String s = "";
                source.get();
                if (source.refer() != null && source.refer() == '\"'){
                    s += source.get();
                    if (source.refer() != null && source.refer() == '\"'){
                        source.get();
                        s = "";
                        String space = "";
                        String buf = "";

                        int line = source.line();

                        space += source.readForward(WHITE_SPACE_CHARACTERS);
                        if (LINE_TERMINATE_CHARACTERS.contains(source.refer())){
                            space += getLineTerminator(source);
                        }else{
                            throw new LexerException("Invalid text block.");
                        }

                        while (true){
                            buf += getCharacter(source);
                            switch (buf){
                                case "\"", "\"\"":
                                    continue;

                                case "\"\"\"":
                                    return new TextBlock(space, s, recorder.get(line, source));

                                default:
                                    s += buf;
                                    buf = "";
                            }
                        }
                    }else{
                        return new StringLiteral("", recorder.get(source));
                    }
                }else{
                    while (source.refer() != '\"'){
                        s += getCharacter(source, '\r', '\n', '\"');
                    }
                    source.get();
                    return new StringLiteral(s, recorder.get(source));
                }
            }

        },

        SEPARATOR_INTRO(c -> SEPARATOR_INTRODUCTION_CHARACTERS.contains(c)){

            @Override
            public Separator getInputElement(CharList source, ReferenceProducer recorder){
                String s = "";
                while (!SEPARATORS.contains(s) || SEPARATORS.contains(s + source.refer())){
                    s += source.get();
                }
                return new Separator(s, recorder.get(source));
            }

        },

        OPERATOR_INTRO(c -> OPERATOR_INTRODUCTION_CHARACTERS.contains(c)){

            @Override
            public Operator getInputElement(CharList source, ReferenceProducer recorder){
                String s = "";
                while (!OPERATORS.contains(s) || OPERATORS.contains(s + source.refer())){
                    s += source.get();
                }
                return new Operator(s, recorder.get(source));
            }

        };

        Predicate<Character> tester;

        private Terminal(Predicate<Character> tester){
            this.tester = tester;
        }

        public static Terminal set(char c) throws CompileException, IndexOutOfBoundsException{
            for (Terminal terminal : Terminal.values()){
                if (terminal.tester.test(c)){
                    return terminal;
                }
            }
            throw new LexerException("Detected invalid character \'" + c + "\'.");
        }

        protected abstract InputElement getInputElement(CharList source, ReferenceProducer recorder)
                throws CompileException, IndexOutOfBoundsException;

    }

}
