package nl.jqno.equalsverifier.internal.util;

import static nl.jqno.equalsverifier.testhelpers.Util.coverThePrivateConstructor;

import org.junit.jupiter.api.Test;

public class PrefabValuesApiTest {

    @Test
    public void coverTheConstructor() {
        coverThePrivateConstructor(PrefabValuesApi.class);
    }
}
