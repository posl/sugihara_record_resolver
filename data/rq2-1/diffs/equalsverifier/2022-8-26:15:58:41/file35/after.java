package nl.jqno.equalsverifier.integration.extended_contract;

import static nl.jqno.equalsverifier.internal.testhelpers.Util.defaultEquals;
import static nl.jqno.equalsverifier.internal.testhelpers.Util.defaultHashCode;

import java.util.Objects;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.internal.testhelpers.ExpectedException;
import org.junit.jupiter.api.Test;

public class ExtendedReflexivityTest {

    @Test
    public void succeed_whenEqualsUsesEqualsMethodForObjects() {
        EqualsVerifier.forClass(UsesEqualsMethod.class).verify();
    }

    @Test
    public void fail_whenEqualsUsesDoubleEqualSignForObjects() {
        ExpectedException
            .when(() -> EqualsVerifier.forClass(UsesDoubleEqualSign.class).verify())
            .assertFailure()
            .assertMessageContains("Reflexivity", "== used instead of .equals()", "stringField");
    }

    @Test
    public void succeed_whenEqualsUsesDoubleEqualSignForObject_givenDoubleEqualWarningIsSuppressed() {
        EqualsVerifier
            .forClass(UsesDoubleEqualSign.class)
            .suppress(Warning.REFERENCE_EQUALITY)
            .verify();
    }

    @Test
    public void fail_whenEqualsUsesDoubleEqualSignForBoxedPrimitives() {
        ExpectedException
            .when(() -> EqualsVerifier.forClass(UsesDoubleEqualSignOnBoxedPrimitive.class).verify())
            .assertFailure()
            .assertMessageContains("Reflexivity", "== used instead of .equals()", "integerField");
    }

    @Test
    public void succeed_whenEqualsUsesDoubleEqualSignForBoxedPrimitives_givenDoubleEqualWarningIsSuppressed() {
        EqualsVerifier
            .forClass(UsesDoubleEqualSignOnBoxedPrimitive.class)
            .suppress(Warning.REFERENCE_EQUALITY)
            .verify();
    }

    @Test
    public void succeed_whenEqualsUsesDoubleEqualSignForObject_givenObjectDoesntDeclareEquals() {
        EqualsVerifier.forClass(FieldHasNoEquals.class).verify();
    }

    @Test
    public void succeed_whenEqualsUsesDoubleEqualSignForObject_givenObjectIsAnInterface() {
        EqualsVerifier.forClass(FieldIsInterface.class).verify();
    }

    @Test
    public void succeed_whenEqualsUsesDoubleEqualSignForObject_givenObjectIsAnInterfaceWithEquals() {
        EqualsVerifier.forClass(FieldIsInterfaceWithEquals.class).verify();
    }

    static final class UsesEqualsMethod {

        private final String s;

        public UsesEqualsMethod(String s) {
            this.s = s;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof UsesEqualsMethod)) {
                return false;
            }
            UsesEqualsMethod other = (UsesEqualsMethod) obj;
            return Objects.equals(s, other.s);
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }
    }

    static final class UsesDoubleEqualSign {

        private final String stringField;

        public UsesDoubleEqualSign(String s) {
            this.stringField = s;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof UsesDoubleEqualSign)) {
                return false;
            }
            UsesDoubleEqualSign other = (UsesDoubleEqualSign) obj;
            return stringField == other.stringField;
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }
    }

    static final class UsesDoubleEqualSignOnBoxedPrimitive {

        private final Integer integerField;

        public UsesDoubleEqualSignOnBoxedPrimitive(Integer i) {
            this.integerField = i;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof UsesDoubleEqualSignOnBoxedPrimitive)) {
                return false;
            }
            UsesDoubleEqualSignOnBoxedPrimitive other = (UsesDoubleEqualSignOnBoxedPrimitive) obj;
            return integerField == other.integerField;
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }
    }

    static final class FieldHasNoEquals {

        @SuppressWarnings("unused")
        private final NoEquals field;

        public FieldHasNoEquals(NoEquals field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj) {
            return defaultEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }

        static final class NoEquals {}
    }

    static final class FieldIsInterface {

        @SuppressWarnings("unused")
        private final Interface field;

        public FieldIsInterface(Interface field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj) {
            return defaultEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }

        interface Interface {}
    }

    static final class FieldIsInterfaceWithEquals {

        @SuppressWarnings("unused")
        private final InterfaceWithEquals field;

        public FieldIsInterfaceWithEquals(InterfaceWithEquals field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj) {
            return defaultEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return defaultHashCode(this);
        }

        interface InterfaceWithEquals {
            boolean equals(Object obj);
        }
    }
}
