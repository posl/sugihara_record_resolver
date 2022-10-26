package rm4j.util;

public enum Brackets{
    ROUND_BRACKETS("(", ")"),
    CURLY_BRACKETS("{", "}"),
    SQUARE_BRACKETS("[", "]"),
    ANGLE_BRACKETS("<", ">"),
    DOUBLE_CURLY_BRACKETS("{{", "}}"),
    DOUBLE_SQUARE_BRACKETS("[[", "]]");

    private String left;
    private String right;

    private Brackets(String left, String right){
        this.left = left;
        this.right = right;
    }

    public String addBrackets(String s){
        return left + " " + s + " " + right;
    }
}
