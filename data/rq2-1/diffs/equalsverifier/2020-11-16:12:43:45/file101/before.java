package nl.jqno.equalsverifier.internal.util;

import static nl.jqno.equalsverifier.testhelpers.Util.coverThePrivateConstructor;

import org.junit.Test;

public class ValidationsTest {
    @Test
    public void coverTheConstructor() {
        coverThePrivateConstructor(Validations.class);
    }
}
