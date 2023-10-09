package nl.jqno.equalsverifier.testhelpers.packages.twoincorrect;

public final class Z {

    private final int x;
    private final int y;

    public Z(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Z)) {
            return false;
        }
        Z p = (Z) obj;
        return p.x == x && p.y == y;
    }

    @Override
    public int hashCode() {
        return x + (31 * y);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + x + "," + y;
    }
}
