package rm4j.compiler.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public abstract class Lexer<T> {
    private static final Set<Character> DIGITS = new HashSet<>(
            Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
    private static final Set<Character> HEX_DIGIT_ALPHABETS = new HashSet<>(
            Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'));

    protected static final Set<Character> ESCAPE_SEQUENCE_MARKERS = new HashSet<>(
            Arrays.asList('b', 's', 't', 'n', 'f', 'r', '\"', '\'', '\\', '\n', '\r'));

    protected static boolean isLineTerminateCharacter(char c){
        return c == '\n' || c == '\r';
    }

    protected static boolean isWhiteSpaceCharacter(char c){
        return c == ' ' || c == '\t' || c == '\f';
    }

    protected static boolean isWhiteSpaceOrLineTerminateCharacters(char c){
        return isWhiteSpaceCharacter(c) || isLineTerminateCharacter(c);
    }

    protected static boolean isBinaryDigit(char c){
        return c == '0' || c == '1';
    }

    protected static boolean isOctalDigit(char c){
        return c != '8' && c != '9' && isDigit(c);
    }

    protected static boolean isDigit(char c){
        return DIGITS.contains(c);
    }

    protected static boolean isNonZeroDigit(char c){
        return c != '0' && isDigit(c);
    }

    protected static boolean isHexDigit(char c){
        return isDigit(c) || HEX_DIGIT_ALPHABETS.contains(c);
    }

    protected static boolean isExponentIndicator(char c){
        return c == 'e' || c == 'E';
    }

    protected static boolean isBinaryExponentIndicator(char c){
        return c == 'p' || c == 'P';
    }

    protected static boolean isSign(char c){
        return c == '+' || c == '-';
    }

    protected static boolean isIntegerTypeSuffix(char c){
        return c == 'l' || c == 'L';
    }

    protected static boolean isFloatSuffix(char c){
        return c == 'd' || c == 'D';
    }

    protected static boolean isDoubleSuffix(char c){
        return c == 'd' || c == 'D';
    }

    protected static boolean isEscapeSequenceMarker(char c){
        return ESCAPE_SEQUENCE_MARKERS.contains(c);
    }

    public Deque<T> tokenize(File file) throws IOException{
        return toTokens(getCharBuf(file));
    }

    private CharBuffer getCharBuf(File file) throws IOException{
        if(file.length() > Integer.MAX_VALUE){
            throw new IOException("The size of an input file must not exceed %d bytes.".formatted(Integer.MAX_VALUE));
        }
        CharBuffer buf = CharBuffer.allocate((int)file.length());
        try(FileReader reader = new FileReader(file)){
            reader.read(buf);
        }
        return buf.flip();
    }

    protected abstract Deque<T> toTokens(CharBuffer src);

    @FunctionalInterface
    protected interface CharPredicate{
        boolean test(char c);
    }
}
