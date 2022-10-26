package rm4j.compiler.tree;

public class FatalParserError extends Error{

    public FatalParserError(String fatal){
        System.err.println("\u001b[00;31m\u001b[1mfatal error: \u001b[00;37m\u001b[0m"+fatal);
    }

}
