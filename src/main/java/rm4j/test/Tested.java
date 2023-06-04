package rm4j.test;

/**
 * 
 */
public @interface Tested{
    enum Status{
        MAYBE_OK,
        PROBABLY_OK,
        CLEARLY_OK,
    }

    String date();
    String tester();
    Status confidence();

}
