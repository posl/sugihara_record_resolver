package nl.jqno.equalsverifier.testhelpers.types;

import static nl.jqno.equalsverifier.internal.testhelpers.Util.defaultHashCode;

public class GetClassPoint {

    private final int x;
    private final int y;

    public GetClassPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        GetClassPoint p = (GetClassPoint) obj;
        return p.x == x && p.y == y;
    }

    @Override
    public int hashCode() {
        return defaultHashCode(this);
    }
}
